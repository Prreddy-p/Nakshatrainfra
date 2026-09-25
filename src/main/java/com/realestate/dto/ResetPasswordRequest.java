package com.realestate.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required")
        String resetToken,
        @NotBlank(message = "New password is required")
        String newPassword,
        @NotBlank(message = "Password confirmation is required")
        String confirmPassword) {
}
