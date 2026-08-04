package com.brinza.notary.service;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.Service;
import com.brinza.notary.domain.ServiceTranslation;
import com.brinza.notary.dto.AppointmentConfirmationView;
import com.brinza.notary.dto.BookingRequest;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentBookingServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private AppointmentEmailService appointmentEmailService;

    private AppointmentBookingService service() {
        return new AppointmentBookingService(appointmentRepository, serviceRepository,
                new ServiceCatalogService(serviceRepository), appointmentEmailService);
    }

    @Test
    void unknownServiceIdThrows() {
        when(serviceRepository.findById(42L)).thenReturn(Optional.empty());
        BookingRequest request = requestFor(42L);

        assertThatThrownBy(() -> service().book(request, Locale.of("ro")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bookSavesPendingAppointmentAndNotifiesClient() {
        Service notaryService = new Service(30, true);
        notaryService.addTranslation(new ServiceTranslation("ro", "Autentificare", "desc"));
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(notaryService));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequest request = requestFor(1L);
        AppointmentConfirmationView confirmation = service().book(request, Locale.of("ro"));

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        Appointment saved = captor.getValue();
        assertThat(saved.getClientName()).isEqualTo("Ion Popescu");
        assertThat(saved.getEndedAt()).isEqualTo(saved.getRequestedAt().plusMinutes(30));

        verify(appointmentEmailService).sendBookingReceivedEmail(saved);
        assertThat(confirmation.clientName()).isEqualTo("Ion Popescu");
        assertThat(confirmation.serviceName()).isEqualTo("Autentificare");
    }

    @Test
    void bookAsAdminSavesAppointmentWithoutNotifyingClient() {
        Service notaryService = new Service(30, true);
        notaryService.addTranslation(new ServiceTranslation("ro", "Autentificare", "desc"));
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(notaryService));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequest request = requestFor(1L);
        Long id = service().bookAsAdmin(request);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        Appointment saved = captor.getValue();
        assertThat(saved.getClientName()).isEqualTo("Ion Popescu");
        assertThat(id).isEqualTo(saved.getId());

        verify(appointmentEmailService, org.mockito.Mockito.never()).sendBookingReceivedEmail(any());
    }

    private static BookingRequest requestFor(Long serviceId) {
        BookingRequest request = new BookingRequest();
        request.setClientName("Ion Popescu");
        request.setEmail("ion@example.com");
        request.setPhone("0700000000");
        request.setServiceId(serviceId);
        request.setRequestedAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        request.setNotes("notes");
        return request;
    }
}
