package com.realestate.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PasswordResetMailer {
    private final ObjectProvider<JavaMailSender> senders;
    private final String from;
    private final PasswordResetEmailSettings emailSettings;

    @Autowired
    public PasswordResetMailer(ObjectProvider<JavaMailSender> senders,
            @Value("${app.password-reset.mail-from:}") String from,
            PasswordResetEmailSettings emailSettings) {
        this.senders = senders;
        this.from = from;
        this.emailSettings = emailSettings;
    }

    public PasswordResetMailer(ObjectProvider<JavaMailSender> senders, String from) {
        this.senders = senders;
        this.from = from;
        this.emailSettings = null;
    }

    public void send(String email, String temporaryPassword, long minutes) {
        JavaMailSender sender = senders.getIfAvailable();
        String configuredFrom = emailSettings == null ? from : emailSettings.getEmail();
        if (sender == null || configuredFrom == null || configuredFrom.isBlank()) {
            throw new IllegalStateException("Password reset email is not configured. Contact your administrator.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(configuredFrom);
        message.setTo(email);
        message.setSubject("Nakshatra Infra temporary password");
        message.setText("Your temporary password is: " + temporaryPassword
                + "\n\nSign in using your registered email and assigned role within " + minutes
                + " minutes. You will be asked to choose a new password."
                + "\n\nIf you did not request this, you can ignore this email. Your current password is unchanged.");
        try {
            sender.send(message);
        } catch (MailException exception) {
            throw new IllegalStateException("Could not send the reset email. Please try again later.");
        }
    }
}
