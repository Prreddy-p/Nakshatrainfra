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
        try (var app = start()) {
            String base = "http://localhost:" + app.getWebServer().getPort() + "/api/";
            inputs.forEach((endpoint, body) -> {
                Map<String, Object> saved = http.postForObject(base + endpoint, body, Map.class);
                assertNotNull(saved);
                assertNotNull(saved.get("id"));
                assertFalse(saved.containsKey("password"));
                ids.put(endpoint, (Number) saved.get("id"));
                counts.put(endpoint, http.getForObject(base + endpoint, List.class).size());
            });
            Map<String, Object> edited = new LinkedHashMap<>(inputs.get("properties"));
            edited.put("propertyName", "Updated property");
            http.put(base + "properties/" + ids.get("properties"), edited);
            // JDK HttpURLConnection does not support PATCH; use Java's HTTP client.
            try {
                var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "tasks/" + ids.get("tasks")))
                        .header("Content-Type", "application/json")
                        .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString("{\"status\":\"Completed\"}"))
                        .build();
                var response = java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
                var missing = java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "tasks/999999"))
                        .header("Content-Type", "application/json")
                        .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString("{\"status\":\"Completed\"}"))
                        .build();
                assertEquals(404, java.net.http.HttpClient.newHttpClient().send(missing, java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode());
            } catch (Exception exception) { throw new AssertionError(exception); }
        }
        // Close the whole application and reopen the SAME file, without create/drop.
        try (var app = start()) {
            String base = "http://localhost:" + app.getWebServer().getPort() + "/api/";
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
            var login = http.postForEntity(base + "auth/login", Map.of("username", "persist@example.com", "password", "Persist@123", "role", "Manager"), Map.class);
            assertEquals(HttpStatus.OK, login.getStatusCode());
            assertEquals(ids.get("users").longValue(), ((Number) login.getBody().get("id")).longValue());
            assertFalse(login.getBody().containsKey("password"));
        }
    }
}
