package com.realestate.controller;

import com.realestate.service.LoginGallerySettings;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/settings")
public class PublicSettingsController {
    private final LoginGallerySettings loginGallerySettings;

    public PublicSettingsController(LoginGallerySettings loginGallerySettings) {
        this.loginGallerySettings = loginGallerySettings;
    }

    @GetMapping("/login-gallery")
    public Map<String, Boolean> getLoginGallery() {
        return Map.of("enabled", loginGallerySettings.isEnabled());
    }
}
