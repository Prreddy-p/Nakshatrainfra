package com.realestate.service;

import com.realestate.model.PasswordResetEmailSetting;
import com.realestate.repository.PasswordResetEmailSettingRepository;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetEmailSettings {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final PasswordResetEmailSettingRepository repository;
    private final String defaultEmail;

    public PasswordResetEmailSettings(PasswordResetEmailSettingRepository repository,
            @Value("${app.password-reset.mail-from:}") String defaultEmail) {
        this.repository = repository;
        this.defaultEmail = defaultEmail;
    }

    @Transactional(readOnly = true)
    public String getEmail() {
        return repository.findAll().stream().findFirst().map(PasswordResetEmailSetting::getEmail).orElse(defaultEmail);
    }

    @Transactional
    public String saveEmail(String email) {
        String normalized = email == null ? "" : email.trim();
        if (!normalized.isEmpty() && (normalized.length() > 320 || !EMAIL.matcher(normalized).matches())) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        PasswordResetEmailSetting setting = repository.findAll().stream().findFirst().orElse(null);
        if (normalized.isEmpty()) {
            if (setting != null) repository.delete(setting);
            return defaultEmail;
        }
        if (setting == null) setting = new PasswordResetEmailSetting();
        setting.setEmail(normalized);
        repository.save(setting);
        return normalized;
    }
}
