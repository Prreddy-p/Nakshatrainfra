package com.realestate.controller;

import com.realestate.model.UserAccount;
import com.realestate.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Mock
    private UserAccountRepository repository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private com.realestate.service.PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController authController;

    @Test
    void loginRejectsMissingCredentials() {
        ResponseEntity<?> response = authController.login(new AuthController.LoginRequest(null, "secret", "Admin"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("message", "Username, password, and role are required"), response.getBody());
        verifyNoInteractions(repository);
    }

    @Test
    void loginTrimsUsernameAndReturnsMatchingAccount() {
        UserAccount account = new UserAccount();
        account.setEmailId("agent@example.com");
        account.setPassword("encoded-password");
        when(passwordEncoder.matches("secret", "encoded-password")).thenReturn(true);
        when(repository.findByEmailIdIgnoreCaseAndRole("agent@example.com", "Agent"))
                .thenReturn(Optional.of(account));

        ResponseEntity<?> response = authController.login(
                new AuthController.LoginRequest("  agent@example.com  ", "secret", "Agent"));

        assertEquals(200, response.getStatusCode().value());
        assertSame(account, response.getBody());
        verify(repository).findByEmailIdIgnoreCaseAndRole("agent@example.com", "Agent");
    }

    @Test
    void loginReturnsUnauthorizedWhenCredentialsDoNotMatch() {
        when(repository.findByEmailIdIgnoreCaseAndRole("agent@example.com", "Agent"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.login(
                new AuthController.LoginRequest("agent@example.com", "wrong", "Agent"));

        assertEquals(401, response.getStatusCode().value());
        assertEquals(Map.of("message", "Invalid username, password, or role"), response.getBody());
    }

    @Test
    void loginRejectsBlankRole() {
        ResponseEntity<?> response = authController.login(
                new AuthController.LoginRequest("agent@example.com", "secret", " "));

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(repository);
    }
}
