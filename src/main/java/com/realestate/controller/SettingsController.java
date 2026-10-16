package com.realestate.controller;

import com.realestate.service.PasswordResetEmailSettings;
import com.realestate.service.LoginGallerySettings;
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
    private final LoginGallerySettings loginGallerySettings;

    public SettingsController(PasswordResetEmailSettings settings, LoginGallerySettings loginGallerySettings) {
        this.settings = settings;
        this.loginGallerySettings = loginGallerySettings;
    }

    @GetMapping("/password-reset-email")
    public Map<String, String> getPasswordResetEmail() {
        return Map.of("email", settings.getEmail());
    }

    @PutMapping("/password-reset-email")
    public Map<String, String> setPasswordResetEmail(@RequestBody Map<String, String> request) {
        return Map.of("email", settings.saveEmail(request.get("email")));
    }

    @PutMapping("/login-gallery")
    public Map<String, Boolean> setLoginGallery(@RequestBody Map<String, Boolean> request) {
        Boolean enabled = request.get("enabled");
        if (enabled == null) throw new IllegalArgumentException("Choose whether login page images are enabled.");
        return Map.of("enabled", loginGallerySettings.setEnabled(enabled));
    }
}
