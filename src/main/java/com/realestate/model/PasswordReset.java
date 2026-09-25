package com.realestate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "password_resets")
public class PasswordReset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(length = 64)
    private String otpHash;

    @Column(length = 512)
    private String encryptedOtp;

    @Column(nullable = false)
    private Instant otpExpiresAt;

    @Column(nullable = false)
    private int otpAttempts;

    @Column(length = 64)
    private String resetTokenHash;

    private Instant resetTokenExpiresAt;
    private Instant otpVerifiedAt;
    private Instant consumedAt;

    @Column(nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount value) { user = value; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String value) { otpHash = value; }
    public String getEncryptedOtp() { return encryptedOtp; }
    public void setEncryptedOtp(String value) { encryptedOtp = value; }
    public Instant getOtpExpiresAt() { return otpExpiresAt; }
    public void setOtpExpiresAt(Instant value) { otpExpiresAt = value; }
    public int getOtpAttempts() { return otpAttempts; }
    public void setOtpAttempts(int value) { otpAttempts = value; }
    public String getResetTokenHash() { return resetTokenHash; }
    public void setResetTokenHash(String value) { resetTokenHash = value; }
    public Instant getResetTokenExpiresAt() { return resetTokenExpiresAt; }
    public void setResetTokenExpiresAt(Instant value) { resetTokenExpiresAt = value; }
    public Instant getOtpVerifiedAt() { return otpVerifiedAt; }
    public void setOtpVerifiedAt(Instant value) { otpVerifiedAt = value; }
    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant value) { consumedAt = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
}
