package com.brinza.notary.service;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.Service;
import com.brinza.notary.dto.AppointmentConfirmationView;
import com.brinza.notary.dto.BookingRequest;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

@org.springframework.stereotype.Service
public class AppointmentBookingService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentBookingService.class);

    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final AppointmentEmailService appointmentEmailService;

    public AppointmentBookingService(AppointmentRepository appointmentRepository,
                                      ServiceRepository serviceRepository,
                                      ServiceCatalogService serviceCatalogService,
                                      AppointmentEmailService appointmentEmailService) {
        this.appointmentRepository = appointmentRepository;
        this.serviceRepository = serviceRepository;
        this.serviceCatalogService = serviceCatalogService;
        this.appointmentEmailService = appointmentEmailService;
    }

    @Transactional
    public AppointmentConfirmationView book(BookingRequest request, Locale locale) {
        Appointment saved = createAppointment(request, null);
        appointmentEmailService.sendBookingReceivedEmail(saved);

        String serviceName = serviceCatalogService.resolveName(saved.getService(), locale);
        return new AppointmentConfirmationView(saved.getClientName(), serviceName, saved.getRequestedAt());
    }

    /**
     * Same as {@link #book(BookingRequest, Locale)} but for appointments an admin creates
     * directly on behalf of a client - unlike a public booking, this does not trigger
     * {@link AppointmentEmailService#sendBookingReceivedEmail}, and the admin picks an explicit
     * {@code endTime} rather than it being derived from the service's duration.
     */
    @Transactional
    public Long bookAsAdmin(BookingRequest request, LocalTime endTime) {
        if (endTime == null) {
            throw new IllegalArgumentException("Ora de sfârșit este obligatorie.");
        }
        return createAppointment(request, endTime).getId();
    }

    private Appointment createAppointment(BookingRequest request, LocalTime explicitEndTime) {
        Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown service id " + request.getServiceId()));

        LocalDateTime endedAt = explicitEndTime != null
                ? LocalDateTime.of(request.getRequestedAt().toLocalDate(), explicitEndTime)
                : request.getRequestedAt().plusMinutes(service.getDurationMinutes());
        if (!endedAt.isAfter(request.getRequestedAt())) {
            throw new IllegalArgumentException("Ora de sfârșit trebuie să fie după ora de început.");
        }

        Appointment appointment = new Appointment(
                request.getClientName(),
                request.getEmail(),
                request.getPhone(),
                service,
                request.getRequestedAt(),
                endedAt,
                request.getNotes()
        );
        Appointment saved = appointmentRepository.save(appointment);
        log.debug("Saved appointment id={} with status={}", saved.getId(), saved.getStatus());
        return saved;
    }
}
