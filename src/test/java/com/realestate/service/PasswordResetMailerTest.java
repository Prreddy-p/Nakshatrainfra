package com.realestate.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PasswordResetMailerTest {
    @SuppressWarnings("unchecked")
    @Test void sendsToRegisteredAddressAndReportsDeliveryFailure() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        PasswordResetMailer mailer = new PasswordResetMailer(provider, "sender@gmail.com");
        mailer.send("registered@example.com", "test-temporary-secret", 15);
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertArrayEquals(new String[]{"registered@example.com"}, message.getValue().getTo());
        assertEquals("sender@gmail.com", message.getValue().getFrom());
        assertTrue(message.getValue().getText().contains("test-temporary-secret"));
        assertTrue(message.getValue().getText().contains("15 minutes"));
        doThrow(new MailSendException("private SMTP details")).when(sender).send(any(SimpleMailMessage.class));
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> mailer.send("registered@example.com", "secret", 15));
        assertFalse(failure.getMessage().contains("private SMTP"));
    }

    @SuppressWarnings("unchecked")
    @Test void missingConfigurationDoesNotAttemptDelivery() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        PasswordResetMailer mailer = new PasswordResetMailer(provider, "");
        assertThrows(IllegalStateException.class, () -> mailer.send("registered@example.com", "secret", 15));
    }
}
