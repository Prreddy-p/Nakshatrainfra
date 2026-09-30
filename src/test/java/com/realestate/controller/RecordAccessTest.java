package com.realestate.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.model.UserAccount;
import com.realestate.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:record-access-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class RecordAccessTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository users;
    @Autowired PasswordEncoder encoder;
    MockHttpSession login(String role) throws Exception {
        var user = new UserAccount(); user.setName("Same name"); user.setRole(role);
        user.setEmailId(java.util.UUID.randomUUID() + "@example.com"); user.setPassword(encoder.encode("Password@123"));
        users.save(user);
        return (MockHttpSession) mvc.perform(post("/api/auth/login").contentType("application/json")
            .content(json.writeValueAsString(Map.of("username", user.getEmailId(), "password", "Password@123", "role", role))))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    JsonNode create(String path, MockHttpSession session, Map<String, Object> body) throws Exception {
        return json.readTree(mvc.perform(post(path).session(session).contentType("application/json")
            .content(json.writeValueAsString(body))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    @Test void associatesSeeOnlyTheirOwnRecordsAndManagersSeeAll() throws Exception {
        var first = login("Associate"); var second = login("Associate"); var manager = login("Manager"); var admin = login("Admin");
        var payloads = Map.<String, Map<String, Object>>of(
            "leads", Map.of("customerName", "Private lead", "createdByUserId", second.getAttribute("accountId")),
            "properties", Map.of("propertyName", "Private property", "city", "Hyderabad"),
            "tasks", Map.of("taskName", "Private task"),
            "payments", Map.of("customer", "Private customer", "amount", 100),
            "documents", Map.of("documentName", "Private document"));
        var ids = new java.util.HashMap<String, Long>();
        for (var entry : payloads.entrySet()) {
            var own = create("/api/" + entry.getKey(), first, entry.getValue());
            ids.put(entry.getKey(), own.get("id").asLong());
            assertEquals((Long) first.getAttribute("accountId"), own.get("createdByUserId").asLong());
            var other = create("/api/" + entry.getKey(), second, entry.getValue());
            String path = "/api/" + entry.getKey();
            var rows = json.readTree(mvc.perform(get(path).session(first)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertEquals(1, rows.size(), path); assertEquals(own.get("id"), rows.get(0).get("id"));
            for (var elevated : java.util.List.of(manager, admin)) {
                var all = json.readTree(mvc.perform(get(path).session(elevated)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
                assertTrue(all.size() >= 2, path);
            }
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        long leadId = ids.get("leads"); long propertyId = ids.get("properties");
        mvc.perform(get("/api/leads/" + leadId).session(second)).andExpect(status().isNotFound());
        mvc.perform(get("/api/leads/" + leadId + "/payments").session(second)).andExpect(status().isNotFound());
        mvc.perform(put("/api/leads/" + leadId + "/payments").session(second).contentType("application/json")
            .content("{}" )).andExpect(status().isNotFound());
        mvc.perform(put("/api/leads/" + leadId).session(second).contentType("application/json")
            .content(json.writeValueAsString(Map.of("customerName", "Stolen")))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/leads/" + leadId).session(second)).andExpect(status().isNotFound());
        mvc.perform(get("/api/properties/" + propertyId).session(second)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/properties/" + propertyId).session(second)).andExpect(status().isNotFound());
        mvc.perform(put("/api/properties/" + propertyId).session(second).contentType("application/json")
            .content(json.writeValueAsString(Map.of("propertyName", "Stolen")))).andExpect(status().isNotFound());
        mvc.perform(patch("/api/tasks/" + ids.get("tasks")).session(second).contentType("application/json")
            .content(json.writeValueAsString(Map.of("status", "Completed")))).andExpect(status().isNotFound());
        mvc.perform(get("/api/users").session(first)).andExpect(status().isForbidden());
        mvc.perform(post("/api/users").session(first).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/leads?category=Warm").session(first)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/properties?search=Private").session(first)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/tasks?status=Pending").session(first)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/dashboard").session(first)).andExpect(jsonPath("totalLeads").value(1))
            .andExpect(jsonPath("totalProperties").value(1));
        // A manager can edit/convert an associate's lead without taking ownership.
        mvc.perform(put("/api/leads/" + leadId + "?conversionConfirmed=true").session(manager).contentType("application/json")
            .content(json.writeValueAsString(Map.of("customerName", "Converted", "advancePaidEnabled", true, "advanceAmount", 100))))
            .andExpect(status().isOk()).andExpect(jsonPath("createdByUserId").value(first.getAttribute("accountId")));
        mvc.perform(get("/api/customers").session(first)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/customers").session(second)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/leads/" + leadId + "/payments").session(first)).andExpect(status().isOk());
        var booking = create("/api/bookings", first, Map.of("bookingId", "OWN-BOOKING", "property", Map.of("id", propertyId)));
        long bookingId = booking.get("id").asLong();
        mvc.perform(get("/api/bookings").session(second)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/bookings/" + bookingId).session(second)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/bookings/" + bookingId + "/status").session(second).contentType("application/json")
            .content(json.writeValueAsString(Map.of("status", "Cancelled")))).andExpect(status().isNotFound());
        mvc.perform(post("/api/bookings").session(second).contentType("application/json")
            .content(json.writeValueAsString(Map.of("bookingId", "STOLEN", "property", Map.of("id", propertyId)))))
            .andExpect(status().isNotFound());
        // Changing a user's role in the database takes effect on their next request.
        var changed = users.findById((Long) manager.getAttribute("accountId")).orElseThrow();
        changed.setRole("Associate"); users.save(changed);
        mvc.perform(get("/api/leads/" + leadId).session(manager)).andExpect(status().isNotFound());
        users.deleteById((Long) second.getAttribute("accountId"));
        mvc.perform(get("/api/leads").session(second)).andExpect(status().isUnauthorized());
    }
}
