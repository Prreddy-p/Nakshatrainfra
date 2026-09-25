package com.realestate.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordValidator {
    private static final Pattern COMPLEX_PASSWORD = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

    public void validate(String password) {
        if (password == null || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
                || password.chars().anyMatch(Character::isWhitespace)
                || !COMPLEX_PASSWORD.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Password must be 8 or more characters, at most 72 UTF-8 bytes, with uppercase, lowercase, number, special character, and no whitespace");
        }
    }
}
