package com.realestate.controller;

import com.realestate.model.UserAccount;
import com.realestate.repository.UserAccountRepository;
import com.realestate.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;

    public AuthController(UserAccountRepository repository, PasswordEncoder passwordEncoder, PasswordResetService passwordResetService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.username() == null || request.password() == null || request.role() == null || request.role().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username, password, and role are required"));
        }
        UserAccount user = repository.findByEmailIdIgnoreCaseAndRole(request.username().trim(), request.role())
                .orElse(null);
        if (user != null) {
            String token = passwordResetService.loginWithTemporaryPassword(user, request.password());
            if (token != null) return ResponseEntity.ok(Map.of("passwordChangeRequired", true, "resetToken", token));
        }
        if (user != null && passwordMatchesOrMigrates(user, request.password())) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(401).body(Map.of("message", "Invalid username, password, or role"));
    }

    private boolean passwordMatchesOrMigrates(UserAccount user, String rawPassword) {
        if (user.getPassword() != null && passwordEncoder.matches(rawPassword, user.getPassword())) {
            return true;
        }
        if (rawPassword.equals(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            repository.save(user);
            return true;
        }
        return false;
    }

    public record LoginRequest(String username, String password, String role) { }
}
