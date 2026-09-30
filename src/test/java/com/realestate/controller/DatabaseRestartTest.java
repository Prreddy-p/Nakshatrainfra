package com.realestate.controller;

import com.realestate.RealEstateApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseRestartTest {
    @TempDir Path directory;
    private final RestTemplate http = new RestTemplate();
    private String cookie;
    private void login(String base) {
        http.getInterceptors().clear();
        var response = http.postForEntity(base + "auth/login", Map.of("username", "user@example.com",
            "password", "DemoPass@123", "role", "Manager"), Map.class);
        cookie = response.getHeaders().get("Set-Cookie").stream().filter(value -> value.startsWith("JSESSIONID=")).reduce((first, last) -> last).orElseThrow().split(";", 2)[0];
        http.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("Cookie", cookie);
            var result = execution.execute(request, body);
            rememberCookies(result.getHeaders().get("Set-Cookie"));
            return result;
        });
    }

    private void rememberCookies(java.util.List<String> values) {
        if (values != null) values.stream().filter(value -> value.startsWith("JSESSIONID="))
            .forEach(value -> cookie = value.split(";", 2)[0]);
    }

    private ServletWebServerApplicationContext start() {
        return (ServletWebServerApplicationContext) new SpringApplicationBuilder(RealEstateApplication.class).run(
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:file:" + directory.resolve("restart-db").toAbsolutePath().toString().replace('\\', '/') + ";WRITE_DELAY=0",
                "--spring.datasource.username=sa", "--spring.datasource.password=",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.jpa.hibernate.ddl-auto=update",
                "--logging.level.root=ERROR", "--logging.level.org.springframework=ERROR",
                "--logging.level.org.hibernate=ERROR");
    }

    @SuppressWarnings("unchecked")
    @Test
    void recordsAndUpdatesSurviveCompleteApplicationRestart() {
        Map<String, Map<String, Object>> inputs = new LinkedHashMap<>();
        inputs.put("users", Map.of("name", "Persistent user", "emailId", "persist@example.com", "password", "Persist@123", "role", "Manager"));
        inputs.put("leads", Map.of("customerName", "Persistent lead", "mobileNumber", "9876543210"));
        inputs.put("properties", Map.of("propertyName", "Persistent property", "city", "Hyderabad", "price", 100000));
        inputs.put("tasks", Map.of("taskName", "Persistent task", "assignedTo", "Manager"));
        inputs.put("payments", Map.of("customer", "Persistent lead", "amount", 1000, "status", "Paid"));
        inputs.put("documents", Map.of("documentName", "Persistent document", "fileUrl", "https://example.com/document.pdf"));
        Map<String, Number> ids = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        String attachmentId;
        byte[] attachmentContent = "Persisted property document".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (var app = start()) {
            String base = "http://localhost:" + app.getWebServer().getPort() + "/api/";
            login(base);
            inputs.forEach((endpoint, body) -> {
                Map<String, Object> saved = http.postForObject(base + endpoint, body, Map.class);
                assertNotNull(saved);
                assertNotNull(saved.get("id"));
                assertFalse(saved.containsKey("password"));
                ids.put(endpoint, (Number) saved.get("id"));
                counts.put(endpoint, http.getForObject(base + endpoint, List.class).size());
            });
            var upload = new org.springframework.util.LinkedMultiValueMap<String, Object>();
            upload.add("files", new org.springframework.core.io.ByteArrayResource(attachmentContent) {
                @Override public String getFilename() { return "property.txt"; }
            });
            var uploadHeaders = new HttpHeaders(); uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            var files = http.postForObject(base + "properties/" + ids.get("properties") + "/attachments",
                new HttpEntity<>(upload, uploadHeaders), List.class);
            attachmentId = (String) ((Map<?, ?>) files.get(0)).get("id");
            Map<String, Object> edited = new LinkedHashMap<>(inputs.get("properties"));
            edited.put("propertyName", "Updated property");
            http.put(base + "properties/" + ids.get("properties"), edited);
            // JDK HttpURLConnection does not support PATCH; use Java's HTTP client.
            try {
                var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "tasks/" + ids.get("tasks")))
                        .header("Content-Type", "application/json").header("Cookie", cookie)
                        .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString("{\"status\":\"Completed\"}"))
                        .build();
                var response = java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                rememberCookies(response.headers().allValues("Set-Cookie"));
                assertEquals(200, response.statusCode());
                var missing = java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "tasks/999999"))
                        .header("Content-Type", "application/json").header("Cookie", cookie)
                        .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString("{\"status\":\"Completed\"}"))
                        .build();
                var missingResponse = java.net.http.HttpClient.newHttpClient().send(missing, java.net.http.HttpResponse.BodyHandlers.discarding());
                rememberCookies(missingResponse.headers().allValues("Set-Cookie"));
                assertEquals(404, missingResponse.statusCode());
            } catch (Exception exception) { throw new AssertionError(exception); }
        }
        // Close the whole application and reopen the SAME file, without create/drop.
        try (var app = start()) {
            String base = "http://localhost:" + app.getWebServer().getPort() + "/api/";
            login(base);
            inputs.forEach((endpoint, body) -> {
                List<Map<String, Object>> loaded = http.getForObject(base + endpoint, List.class);
                assertEquals(counts.get(endpoint), loaded.size(), endpoint + " must not lose or duplicate rows");
                Map<String, Object> row = loaded.stream()
                        .filter(item -> ((Number) item.get("id")).longValue() == ids.get(endpoint).longValue())
                        .findFirst().orElseThrow();
                if (endpoint.equals("properties")) assertEquals("Updated property", row.get("propertyName"));
                if (endpoint.equals("tasks")) {
                    assertEquals("Completed", row.get("status"));
                    assertEquals("Persistent task", row.get("taskName"));
                    assertEquals("Manager", row.get("assignedTo"));
                }
                if (endpoint.equals("users")) assertFalse(row.containsKey("password"));
            });
            assertArrayEquals(attachmentContent, http.getForObject(base + "properties/" + ids.get("properties") + "/attachments/" + attachmentId, byte[].class));
            var login = http.postForEntity(base + "auth/login", Map.of("username", "persist@example.com", "password", "Persist@123", "role", "Manager"), Map.class);
            assertEquals(HttpStatus.OK, login.getStatusCode());
            assertEquals(ids.get("users").longValue(), ((Number) login.getBody().get("id")).longValue());
            assertFalse(login.getBody().containsKey("password"));
        }
    }
}
