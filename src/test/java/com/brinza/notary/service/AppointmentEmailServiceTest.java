package com.brinza.notary.service;

import com.brinza.notary.config.SystemSettings;
import com.brinza.notary.config.properties.ContactSettings;
import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.Service;
import com.brinza.notary.domain.ServiceTranslation;
import com.brinza.notary.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentEmailServiceTest {

    @Mock
    private MessageSource messageSource;
    @Mock
    private AsyncEmailSender asyncEmailSender;
    @Mock
    private SystemSettings systemSettings;
    @Mock
    private ServiceRepository serviceRepository;

    private AppointmentEmailService service() {
        ContactSettings contactSettings = new ContactSettings("Str. Test 1", "Test City", "111111", "RO",
                "0700000000", "test@example.com", "09:00", "17:00", java.util.List.of("Monday"), 46.0, 23.0);
        return new AppointmentEmailService(messageSource, new ServiceCatalogService(serviceRepository),
                asyncEmailSender, systemSettings, "office@example.com", contactSettings);
    }

    private Appointment appointment() {
        Service notaryService = new Service(30, true);
        notaryService.addTranslation(new ServiceTranslation("ro", "Autentificare", "desc"));
        notaryService.addTranslation(new ServiceTranslation("en", "Authentication", "desc"));
        notaryService.addTranslation(new ServiceTranslation("hu", "Hitelesites", "desc"));
        return new Appointment("Ion Popescu", "ion@example.com", "0711111111", notaryService,
                LocalDateTime.of(2026, 8, 1, 10, 0), LocalDateTime.of(2026, 8, 1, 10, 30), "notes");
    }

    @Test
    void doesNotSendWhenMailDisabled() {
        when(systemSettings.isMailEnabled()).thenReturn(false);

        service().sendBookingReceivedEmail(appointment());

        verify(asyncEmailSender, never()).sendAsync(any(), any());
    }

    @Test
    void sendsBookingReceivedEmailWhenMailEnabled() {
        when(systemSettings.isMailEnabled()).thenReturn(true);
        stubMessages();

        Appointment appointment = appointment();
        service().sendBookingReceivedEmail(appointment);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(asyncEmailSender).sendAsync(captor.capture(), org.mockito.ArgumentMatchers.eq(appointment.getId()));
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("office@example.com");
        assertThat(message.getTo()).containsExactly("ion@example.com");
        assertThat(message.getSubject()).contains("[email.booking-received.subject:ro]")
                .contains("[email.booking-received.subject:en]")
                .contains("[email.booking-received.subject:hu]");
        assertThat(message.getText()).contains("[email.booking-received.body:ro]")
                .contains("[email.booking-received.body:en]")
                .contains("[email.booking-received.body:hu]");
    }

    @Test
    void sendsConfirmedEmailWhenMailEnabled() {
        when(systemSettings.isMailEnabled()).thenReturn(true);
        stubMessages();

        Appointment appointment = appointment();
        service().sendConfirmedEmail(appointment);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(asyncEmailSender).sendAsync(captor.capture(), org.mockito.ArgumentMatchers.eq(appointment.getId()));
        assertThat(captor.getValue().getSubject()).contains("[email.confirmed.subject:ro]");
    }

    private void stubMessages() {
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    Locale locale = invocation.getArgument(2);
                    return "[" + key + ":" + locale.getLanguage() + "]";
                });
    }
}
