package com.realestate.dto;

public record AuthResponse(String message, String resetToken) {
    public static AuthResponse message(String value) {
        return new AuthResponse(value, null);
    }

    public static AuthResponse resetToken(String value) {
        return new AuthResponse("OTP verified. Use the reset token to choose a new password.", value);
    }
}
