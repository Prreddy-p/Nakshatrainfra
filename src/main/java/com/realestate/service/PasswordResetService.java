package com.realestate.service;

import com.realestate.dto.AuthResponse;
import com.realestate.dto.DevOtpResponse;
import com.realestate.dto.ForgotPasswordRequest;
import com.realestate.dto.ResetPasswordRequest;
import com.realestate.dto.VerifyOtpRequest;
import com.realestate.exception.ResetAuthenticationException;
import com.realestate.exception.TooManyRequestsException;
import com.realestate.model.PasswordReset;
import com.realestate.model.UserAccount;
import com.realestate.repository.PasswordResetRepository;
import com.realestate.repository.UserAccountRepository;
import com.realestate.security.PasswordResetCrypto;
import com.realestate.security.PasswordValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

@Service
public class PasswordResetService {
    private static final String GENERIC_FORGOT_MESSAGE =
            "If an account exists for that email, a temporary password has been sent to its registered email address.";
    private static final String INVALID_OTP_MESSAGE = "The OTP is invalid or has expired";
    private static final String INVALID_TOKEN_MESSAGE = "The reset token is invalid or has expired";

    private final UserAccountRepository userRepository;
    private final PasswordResetRepository resetRepository;
    private final PasswordResetCrypto crypto;
    private final PasswordValidator passwordValidator;
    private final PasswordEncoder passwordEncoder;
    private final Duration otpExpiry;
    private final Duration tokenExpiry;
    private final Duration requestWindow;
    private final int maxAttempts;
    private final boolean devOtpEnabled;
    private final PasswordResetMailer mailer;

    public PasswordResetService(
            UserAccountRepository userRepository,
            PasswordResetRepository resetRepository,
            PasswordResetCrypto crypto,
            PasswordValidator passwordValidator,
            PasswordEncoder passwordEncoder,
            PasswordResetMailer mailer,
            @Value("${app.password-reset.otp-expiry}") Duration otpExpiry,
            @Value("${app.password-reset.token-expiry}") Duration tokenExpiry,
            @Value("${app.password-reset.request-window}") Duration requestWindow,
            @Value("${app.password-reset.max-attempts}") int maxAttempts,
            @Value("${app.password-reset.dev-otp-enabled}") boolean devOtpEnabled) {
        this.userRepository = userRepository;
        this.resetRepository = resetRepository;
        this.crypto = crypto;
        this.passwordValidator = passwordValidator;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.otpExpiry = otpExpiry;
        this.tokenExpiry = tokenExpiry;
        this.requestWindow = requestWindow;
        this.maxAttempts = maxAttempts;
        this.devOtpEnabled = devOtpEnabled;
    }

    @Transactional
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = userRepository.lockByEmail(email).orElse(null);
        if (user == null) {
            return AuthResponse.message(GENERIC_FORGOT_MESSAGE);
        }

        Instant now = Instant.now();
        resetRepository.findTopByUser_EmailIdIgnoreCaseOrderByCreatedAtDesc(email)
                .filter(previous -> previous.getCreatedAt().isAfter(now.minus(requestWindow)))
                .ifPresent(previous -> {
                    throw new TooManyRequestsException("Too many reset requests. Try again later.");
                });

        String otp = crypto.randomResetToken();
        PasswordReset reset = new PasswordReset();
        reset.setUser(user);
        reset.setOtpHash(crypto.hash(otp));
        reset.setOtpExpiresAt(now.plus(otpExpiry));
        reset.setOtpAttempts(0);
        reset.setCreatedAt(now);
        for (PasswordReset previous : resetRepository.findByUserAndConsumedAtIsNull(user)) {
            previous.setConsumedAt(now);
            previous.setResetTokenHash(null);
            previous.setOtpHash(null);
            previous.setEncryptedOtp(null);
            resetRepository.save(previous);
        }
        resetRepository.saveAndFlush(reset);
        mailer.send(user.getEmailId(), otp, otpExpiry.toMinutes());
        return AuthResponse.message(GENERIC_FORGOT_MESSAGE);
    }

    @Transactional
    public String loginWithTemporaryPassword(UserAccount user, String password) {
        PasswordReset reset = resetRepository.findTopByUser_EmailIdIgnoreCaseOrderByCreatedAtDesc(user.getEmailId())
                .orElse(null);
        Instant now = Instant.now();
        if (reset == null || reset.getConsumedAt() != null || reset.getOtpVerifiedAt() != null
                || !reset.getOtpExpiresAt().isAfter(now) || reset.getOtpHash() == null) return null;
        if (!MessageDigest.isEqual(reset.getOtpHash().getBytes(StandardCharsets.UTF_8),
                crypto.hash(password).getBytes(StandardCharsets.UTF_8))) return null;
        String token = crypto.randomResetToken();
        reset.setResetTokenHash(crypto.hash(token));
        reset.setResetTokenExpiresAt(now.plus(tokenExpiry));
        reset.setOtpVerifiedAt(now);
        reset.setOtpHash(null);
        resetRepository.save(reset);
        return token;
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String email = normalizeEmail(request.email());
        PasswordReset reset = latestReset(email);
        Instant now = Instant.now();
        if (reset.getConsumedAt() != null || reset.getOtpVerifiedAt() != null || reset.getOtpExpiresAt().isBefore(now)) {
            throw new ResetAuthenticationException(INVALID_OTP_MESSAGE);
        }
        if (reset.getOtpAttempts() >= maxAttempts) {
            throw new TooManyRequestsException("Too many invalid OTP attempts. Request a new OTP.");
        }

        reset.setOtpAttempts(reset.getOtpAttempts() + 1);
        if (!MessageDigest.isEqual(
                reset.getOtpHash().getBytes(StandardCharsets.UTF_8),
                crypto.hash(request.otp()).getBytes(StandardCharsets.UTF_8))) {
            resetRepository.save(reset);
            if (reset.getOtpAttempts() >= maxAttempts) {
                throw new TooManyRequestsException("Too many invalid OTP attempts. Request a new OTP.");
            }
            throw new ResetAuthenticationException(INVALID_OTP_MESSAGE);
        }

        String resetToken = crypto.randomResetToken();
        reset.setResetTokenHash(crypto.hash(resetToken));
        reset.setResetTokenExpiresAt(now.plus(tokenExpiry));
        reset.setOtpVerifiedAt(now);
        resetRepository.save(reset);
        return AuthResponse.resetToken(resetToken);
    }

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation must match");
        }
        passwordValidator.validate(request.newPassword());

        PasswordReset reset = resetRepository.findByResetTokenHashAndConsumedAtIsNull(
                        crypto.hash(request.resetToken()))
                .orElseThrow(() -> new ResetAuthenticationException(INVALID_TOKEN_MESSAGE));
        Instant now = Instant.now();
        if (reset.getOtpVerifiedAt() == null
                || reset.getResetTokenExpiresAt() == null
                || reset.getResetTokenExpiresAt().isBefore(now)) {
            throw new ResetAuthenticationException(INVALID_TOKEN_MESSAGE);
        }

        reset.getUser().setPassword(passwordEncoder.encode(request.newPassword()));
        reset.setConsumedAt(now);
        reset.setResetTokenHash(null);
        reset.setOtpHash(null);
        reset.setEncryptedOtp(null);
        userRepository.save(reset.getUser());
        resetRepository.save(reset);
        return AuthResponse.message("Password reset successfully");
    }

    @Transactional(readOnly = true)
    public DevOtpResponse viewDevOtp(String email) {
        if (!devOtpEnabled) {
            throw new ResetAuthenticationException("Development OTP inspection is disabled");
        }
        PasswordReset reset = latestReset(normalizeEmail(email));
        if (reset.getOtpExpiresAt().isBefore(Instant.now()) || reset.getOtpHash() == null || reset.getEncryptedOtp() == null) {
            throw new ResetAuthenticationException(INVALID_OTP_MESSAGE);
        }
        return new DevOtpResponse(reset.getUser().getEmailId(), crypto.decrypt(reset.getEncryptedOtp()), reset.getOtpExpiresAt());
    }

    private PasswordReset latestReset(String email) {
        return resetRepository.findTopByUser_EmailIdIgnoreCaseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new ResetAuthenticationException(INVALID_OTP_MESSAGE));
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
