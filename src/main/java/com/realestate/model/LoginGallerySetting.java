package com.realestate.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "login_gallery_settings")
public class LoginGallerySetting {
    @Id
    private Long id = 1L;

    private boolean enabled;

    public Long getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
