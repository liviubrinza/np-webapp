package com.brinza.notary.service;

import com.brinza.notary.config.SystemSettings;
import com.brinza.notary.domain.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;

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
 * <p>Gated by {@link SystemSettings#isMailEnabled()}, off by default (defaults to
 * {@code app.mail.enabled}, which is {@code false} unless overridden - see
 * {@code application.yml} and {@code application-local-dev.yml}) so a stock checkout never
 * attempts to send real email. Runtime-toggleable from the Configurare admin page without a
 * restart. A send failure (bad credentials, unreachable SMTP server, etc.) is logged and
 * swallowed - it must never fail the appointment operation that triggered it.
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

    private final MessageSource messageSource;
    private final ServiceCatalogService serviceCatalogService;
    private final AsyncEmailSender asyncEmailSender;
    private final SystemSettings systemSettings;
    private final String fromAddress;
    private final String contactAddress;
    private final String contactPhone;

    public AppointmentEmailService(MessageSource messageSource,
                                    ServiceCatalogService serviceCatalogService,
                                    AsyncEmailSender asyncEmailSender,
                                    SystemSettings systemSettings,
                                    @Value("${app.mail.from:}") String fromAddress,
                                    @Value("${app.contact.address}") String contactAddress,
                                    @Value("${app.contact.phone}") String contactPhone) {
        this.messageSource = messageSource;
        this.serviceCatalogService = serviceCatalogService;
        this.asyncEmailSender = asyncEmailSender;
        this.systemSettings = systemSettings;
        this.fromAddress = fromAddress;
        this.contactAddress = contactAddress;
        this.contactPhone = contactPhone;
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
        if (!systemSettings.isMailEnabled()) {
            log.debug("Email sending disabled (system setting mail.enabled=false); skipping appointmentId={}", appointment.getId());
            return;
        }
        SimpleMailMessage message = buildMessage(appointment, subjectKey, bodyKey);
        asyncEmailSender.sendAsync(message, appointment.getId());
    }

    private SimpleMailMessage buildMessage(Appointment appointment, String subjectKey, String bodyKey) {
        String subject = buildTrilingualSubject(subjectKey);
        String body = buildTrilingualBody(appointment, bodyKey);
        log.debug("Composed email for appointmentId={} recipient={} subject={}\n{}",
                appointment.getId(), appointment.getEmail(), subject, body);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(appointment.getEmail());
        message.setSubject(subject);
        message.setText(body);
        return message;
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
            Object[] contactArgs = {this.contactAddress, this.contactPhone};

            body.append("\n");
            body.append(">>> ").append(entry.getValue()).append(" <<<\n\n");
            body.append(messageSource.getMessage(bodyKey, args, locale));
            body.append("\n\n");
            body.append(messageSource.getMessage("email.contactInfo", contactArgs, locale));
            body.append("\n\n");
        }
        return body.toString().strip();
    }
}
