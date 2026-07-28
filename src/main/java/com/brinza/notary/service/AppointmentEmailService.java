package com.brinza.notary.service;

import com.brinza.notary.domain.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Sends the two client-facing appointment emails (booking received, appointment confirmed) as
 * plain text, in Romanian, English, and Hungarian all at once - one section per language,
 * separated by a "--- LANGUAGE ---" marker - since an {@link Appointment} doesn't record which
 * locale the client originally booked in.
 *
 * <p>Gated by {@code app.mail.enabled}, off by default so a stock checkout never attempts to
 * send real email; see {@code application.yml} and {@code application-local-dev.yml}. A send
 * failure (bad credentials, unreachable SMTP server, etc.) is logged and swallowed - it must
 * never fail the appointment operation that triggered it.
 */
@org.springframework.stereotype.Service
public class AppointmentEmailService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEmailService.class);

    private static final Map<Locale, String> EMAIL_LANGUAGES = new LinkedHashMap<>();

    static {
        EMAIL_LANGUAGES.put(Locale.of("ro"), "ROMÂNĂ");
        EMAIL_LANGUAGES.put(Locale.of("en"), "ENGLISH");
        EMAIL_LANGUAGES.put(Locale.of("hu"), "MAGYAR");
    }

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;
    private final ServiceCatalogService serviceCatalogService;
    private final boolean mailEnabled;
    private final String fromAddress;

    public AppointmentEmailService(JavaMailSender mailSender, MessageSource messageSource,
                                    ServiceCatalogService serviceCatalogService,
                                    @Value("${app.mail.enabled:false}") boolean mailEnabled,
                                    @Value("${app.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.messageSource = messageSource;
        this.serviceCatalogService = serviceCatalogService;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
    }

    public void sendBookingReceivedEmail(Appointment appointment) {
        log.info("sendBookingReceivedEmail called for appointmentId={}", appointment.getId());
        send(appointment, "email.booking-received.subject", "email.booking-received.body");
    }

    public void sendConfirmedEmail(Appointment appointment) {
        log.info("sendConfirmedEmail called for appointmentId={}", appointment.getId());
        send(appointment, "email.confirmed.subject", "email.confirmed.body");
    }

    private void send(Appointment appointment, String subjectKey, String bodyKey) {
        if (!mailEnabled) {
            log.debug("Email sending disabled (app.mail.enabled=false); skipping appointmentId={}", appointment.getId());
            return;
        }

        String subject = buildTrilingualSubject(subjectKey);
        String body = buildTrilingualBody(appointment, bodyKey);
        log.debug("Composed email for appointmentId={} recipient={} subject={}\n{}",
                appointment.getId(), appointment.getEmail(), subject, body);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(appointment.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.debug("Sent email for appointmentId={}", appointment.getId());
        } catch (MailException e) {
            log.error("Failed to send email for appointmentId={}: {}", appointment.getId(), e.getMessage());
        }
    }

    private String buildTrilingualSubject(String subjectKey) {
        StringBuilder subject = new StringBuilder();
        for (Locale locale : EMAIL_LANGUAGES.keySet()) {
            if (!subject.isEmpty()) {
                subject.append(" / ");
            }
            subject.append(messageSource.getMessage(subjectKey, null, locale));
        }
        return subject.toString();
    }

    private String buildTrilingualBody(Appointment appointment, String bodyKey) {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<Locale, String> entry : EMAIL_LANGUAGES.entrySet()) {
            Locale locale = entry.getKey();
            String serviceName = serviceCatalogService.resolveName(appointment.getService(), locale);
            String requestedAt = appointment.getRequestedAt()
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", locale));
            Object[] args = {appointment.getClientName(), serviceName, requestedAt};

            body.append("--- ").append(entry.getValue()).append(" ---\n");
            body.append(messageSource.getMessage(bodyKey, args, locale));
            body.append("\n\n");
        }
        return body.toString().strip();
    }
}
