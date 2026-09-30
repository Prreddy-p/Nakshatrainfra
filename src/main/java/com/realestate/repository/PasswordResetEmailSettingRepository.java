package com.realestate.repository;

import com.realestate.model.PasswordResetEmailSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetEmailSettingRepository extends JpaRepository<PasswordResetEmailSetting, Long> {
}
