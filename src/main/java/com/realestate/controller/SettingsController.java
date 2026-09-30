package com.realestate.controller;

import com.realestate.service.PasswordResetEmailSettings;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final PasswordResetEmailSettings settings;

    public SettingsController(PasswordResetEmailSettings settings) { this.settings = settings; }

    @GetMapping("/password-reset-email")
    public Map<String, String> getPasswordResetEmail() {
        return Map.of("email", settings.getEmail());
    }

    @PutMapping("/password-reset-email")
    public Map<String, String> setPasswordResetEmail(@RequestBody Map<String, String> request) {
        return Map.of("email", settings.saveEmail(request.get("email")));
    }
}
