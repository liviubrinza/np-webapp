package com.brinza.notary.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Hands the actual SMTP round-trip off to a separate thread so callers (e.g. an admin confirming
 * an appointment) don't block waiting on mail server latency. Failures are logged and swallowed,
 * same as the synchronous path in {@link AppointmentEmailService} - there's no result for the
 * caller to observe anyway once the call has returned.
 */
@Component
public class AsyncEmailSender {

    private static final Logger log = LoggerFactory.getLogger(AsyncEmailSender.class);

    private final JavaMailSender mailSender;

    public AsyncEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendAsync(SimpleMailMessage message, Long appointmentId) {
        try {
            mailSender.send(message);
            log.debug("Successfully sent email for appointmentId={}", appointmentId);
        } catch (MailException e) {
            log.error("Failed to send email for appointmentId={}: {}", appointmentId, e.getMessage());
        }
    }
}
