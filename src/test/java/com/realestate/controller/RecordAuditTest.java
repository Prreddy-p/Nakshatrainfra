package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static com.realestate.controller.AuthenticatedRequests.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:audit-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class RecordAuditTest {
    @Autowired com.realestate.repository.UserAccountRepository authUsers;
    @org.junit.jupiter.api.BeforeEach void authenticateRequests() { AuthenticatedRequests.manager(authUsers); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    MockHttpSession login(String name, String email) throws Exception {
        mvc.perform(post("/api/users").contentType("application/json").content(json.writeValueAsString(
            Map.of("name", name, "emailId", email, "password", "Password@123", "role", "Manager"))))
            .andExpect(status().isOk());
        return (MockHttpSession) mvc.perform(post("/api/auth/login").contentType("application/json")
            .content(json.writeValueAsString(Map.of("username", email, "password", "Password@123", "role", "Manager"))))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    @Test void preservesCreatorAndSharesAuditWithConvertedCustomer() throws Exception {
        var creator = login("First User", "creator@example.com");
        String result = mvc.perform(post("/api/leads").session(creator).contentType("application/json")
            .content("{\"customerName\":\"Audit lead\",\"createdBy\":\"Forged\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("createdBy").value("First User"))
            .andExpect(jsonPath("lastModifiedBy").value("First User"))
            .andReturn().getResponse().getContentAsString();
        var original = json.readTree(result);
        long id = original.get("id").asLong();
        assertNotNull(java.time.Instant.parse(original.get("createdAt").asText()));
        var editor = login("Second User", "editor@example.com");
        mvc.perform(put("/api/leads/" + id + "?conversionConfirmed=true").session(editor).contentType("application/json")
            .content("{\"customerName\":\"Edited\",\"advancePaidEnabled\":true,\"advanceAmount\":100,\"createdBy\":\"Forged\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("createdBy").value("First User"))
            .andExpect(jsonPath("createdAt").value(original.get("createdAt").asText()))
            .andExpect(jsonPath("lastModifiedBy").value("Second User"));
        mvc.perform(get("/api/customers")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].createdBy").value("First User"))
            .andExpect(jsonPath("$[0].lastModifiedBy").value("Second User"));
        mvc.perform(post("/api/auth/logout").session(editor)).andExpect(status().isNoContent());
        assertTrue(editor.isInvalid());
    }
}
