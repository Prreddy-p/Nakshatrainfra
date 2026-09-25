package com.realestate.dto;

import java.time.Instant;

public record DevOtpResponse(String email, String otp, Instant expiresAt) {
}
