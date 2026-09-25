package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.model.UserAccount;
import com.realestate.model.PasswordReset;
import com.realestate.repository.UserAccountRepository;
import com.realestate.repository.PasswordResetRepository;
import com.realestate.service.PasswordResetMailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:password-reset-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class PasswordResetFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository users;
    @Autowired PasswordResetRepository resets;
    @Autowired PasswordEncoder encoder;
    @MockBean PasswordResetMailer mailer;
    final String email = "reset-test@example.com";

    @BeforeEach void setup() {
        resets.deleteAll();
        users.findByEmailIdIgnoreCase(email).ifPresent(users::delete);
        UserAccount user = new UserAccount();
        user.setName("Reset test"); user.setEmailId(email); user.setRole("Manager");
        user.setPassword(encoder.encode("Original@123"));
        users.save(user);
    }

    String requestTemporaryPassword() throws Exception {
        mvc.perform(post("/api/auth/forgot-password").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email.toUpperCase()))))
                .andExpect(status().isOk()).andExpect(jsonPath("resetToken").isEmpty());
        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq(email), password.capture(), eq(15L));
        return password.getValue();
    }

    org.springframework.test.web.servlet.ResultActions login(String password, String role) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("username", email, "password", password, "role", role))));
    }

    org.springframework.test.web.servlet.ResultActions change(String token, String password, String confirmation) throws Exception {
        return mvc.perform(post("/api/auth/reset-password").contentType("application/json")
                .content(json.writeValueAsString(Map.of("resetToken", token, "newPassword", password, "confirmPassword", confirmation))));
    }

    @Test void fullFlowSavesHashAndRejectsReusedCredentials() throws Exception {
        String temporary = requestTemporaryPassword();
        login(temporary, "Associate").andExpect(status().isUnauthorized());
        String body = login(temporary, "Manager").andExpect(status().isOk())
                .andExpect(jsonPath("passwordChangeRequired").value(true))
                .andExpect(jsonPath("password").doesNotExist()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("resetToken").asText();
        login(temporary, "Manager").andExpect(status().isUnauthorized());
        change(token, "Updated@123", "different").andExpect(status().isBadRequest());
        change(token, "weak", "weak").andExpect(status().isBadRequest());
        change(token, "Updated@123", "Updated@123").andExpect(status().isOk());
        assertTrue(encoder.matches("Updated@123", users.findByEmailIdIgnoreCase(email).orElseThrow().getPassword()));
        login("Updated@123", "Manager").andExpect(status().isOk()).andExpect(jsonPath("emailId").value(email));
        login("Original@123", "Manager").andExpect(status().isUnauthorized());
        login(temporary, "Manager").andExpect(status().isUnauthorized());
        change(token, "Another@123", "Another@123").andExpect(status().isUnauthorized());
    }

    @Test void unknownEmailDoesNotSendOrCreateUser() throws Exception {
        long count = users.count();
        mvc.perform(post("/api/auth/forgot-password").contentType("application/json")
                .content("{\"email\":\"missing@example.com\"}" )).andExpect(status().isOk());
        verifyNoInteractions(mailer);
        assertEquals(count, users.count());
        assertEquals(0, resets.count());
    }

    @Test void emailFailureRollsBackResetAndPreservesPassword() throws Exception {
        doThrow(new IllegalStateException("Could not send the reset email")).when(mailer).send(anyString(), anyString(), anyLong());
        mvc.perform(post("/api/auth/forgot-password").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email)))).andExpect(status().isConflict());
        assertEquals(0, resets.count());
        login("Original@123", "Manager").andExpect(status().isOk());
    }

    @Test void temporaryPasswordExpiresAndRequestsAreThrottled() throws Exception {
        String temporary = requestTemporaryPassword();
        mvc.perform(post("/api/auth/forgot-password").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email)))).andExpect(status().isTooManyRequests());
        PasswordReset reset = resets.findAll().get(0);
        reset.setOtpExpiresAt(Instant.now().minusSeconds(1));
        resets.save(reset);
        login(temporary, "Manager").andExpect(status().isUnauthorized());
    }

    @Test void resetTokenExpires() throws Exception {
        String temporary = requestTemporaryPassword();
        String body = login(temporary, "Manager").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("resetToken").asText();
        PasswordReset reset = resets.findAll().get(0);
        reset.setResetTokenExpiresAt(Instant.now().minusSeconds(1));
        resets.save(reset);
        change(token, "Updated@123", "Updated@123").andExpect(status().isUnauthorized());
    }

    @Test void aNewRequestInvalidatesPreviousResetToken() throws Exception {
        String temporary = requestTemporaryPassword();
        String body = login(temporary, "Manager").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("resetToken").asText();
        PasswordReset previous = resets.findAll().get(0);
        previous.setCreatedAt(Instant.now().minusSeconds(120));
        resets.save(previous);
        clearInvocations(mailer);
        String replacement = requestTemporaryPassword();
        change(token, "Updated@123", "Updated@123").andExpect(status().isUnauthorized());
        login(temporary, "Manager").andExpect(status().isUnauthorized());
        login(replacement, "Manager").andExpect(status().isOk()).andExpect(jsonPath("passwordChangeRequired").value(true));
    }
}
