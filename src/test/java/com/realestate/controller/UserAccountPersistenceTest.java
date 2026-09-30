package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static com.realestate.controller.AuthenticatedRequests.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-persistence-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class UserAccountPersistenceTest {
    @Autowired com.realestate.repository.UserAccountRepository authUsers;
    @org.junit.jupiter.api.BeforeEach void authenticateRequests() { AuthenticatedRequests.manager(authUsers); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void editPreservesPasswordAndSupportsAdminRole() throws Exception {
        String created = mvc.perform(post("/api/users").contentType("application/json")
                .content(json.writeValueAsString(Map.of("name", "Editor", "emailId", "edit-user@example.com",
                        "password", "Original@123", "role", "Manager"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        String hash = users.findById(id).orElseThrow().getPassword();
        mvc.perform(put("/api/users/" + id).contentType("application/json")
                .content(json.writeValueAsString(Map.of("name", "Updated Admin", "emailId", "edited@example.com",
                        "phoneNumber", "1234567890", "password", "", "role", "Admin"))))
                .andExpect(status().isOk()).andExpect(jsonPath("name").value("Updated Admin"))
                .andExpect(jsonPath("password").doesNotExist());
        assertEquals(hash, users.findById(id).orElseThrow().getPassword());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("username", "edited@example.com",
                        "password", "Original@123", "role", "Admin"))))
                .andExpect(status().isOk());
        mvc.perform(put("/api/users/" + id).contentType("application/json")
                .content(json.writeValueAsString(Map.of("name", "Updated Admin", "emailId", "edited@example.com",
                        "password", "short", "role", "Admin"))))
                .andExpect(status().isBadRequest());
        assertEquals(hash, users.findById(id).orElseThrow().getPassword());
    }

    @Test
    void createdUserIsCommittedAndCanSignInWithoutExposingPassword() throws Exception {
        String email = "saved-user@example.com";
        String body = json.writeValueAsString(Map.of("name", "Saved User", "emailId", email,
                "phoneNumber", "9876543210", "password", "SavedUser@123", "role", "Associate"));
        String response = mvc.perform(post("/api/users").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").isNumber())
                .andExpect(jsonPath("password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(response).get("id").asLong();

        // No test transaction: this reads the row committed by the HTTP request.
        var saved = users.findById(id).orElseThrow();
        assertEquals(email, saved.getEmailId());
        assertNotEquals("SavedUser@123", saved.getPassword());
        assertTrue(encoder.matches("SavedUser@123", saved.getPassword()));
        mvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].emailId").value(email))
                .andExpect(jsonPath("$[*].password").doesNotExist());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("username", email,
                                "password", "SavedUser@123", "role", "Associate"))))
                .andExpect(status().isOk()).andExpect(jsonPath("id").value(id))
                .andExpect(jsonPath("password").doesNotExist());
        mvc.perform(post("/api/users").contentType("application/json").content(body))
                .andExpect(status().isConflict());
        assertEquals(id, users.findByEmailIdIgnoreCase(email).orElseThrow().getId());
    }
}
