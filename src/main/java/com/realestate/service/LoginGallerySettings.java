package com.realestate.service;

import com.realestate.model.LoginGallerySetting;
import com.realestate.repository.LoginGallerySettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginGallerySettings {
    private final LoginGallerySettingRepository repository;

    public LoginGallerySettings(LoginGallerySettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isEnabled() {
        return repository.findById(1L).map(LoginGallerySetting::isEnabled).orElse(false);
    }

    @Transactional
    public boolean setEnabled(boolean enabled) {
        LoginGallerySetting setting = repository.findById(1L).orElseGet(LoginGallerySetting::new);
        setting.setEnabled(enabled);
        repository.save(setting);
        return setting.isEnabled();
    }
}
