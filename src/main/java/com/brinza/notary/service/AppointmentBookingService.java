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

import java.util.Locale;

@org.springframework.stereotype.Service
public class AppointmentBookingService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentBookingService.class);

    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceCatalogService serviceCatalogService;

    public AppointmentBookingService(AppointmentRepository appointmentRepository,
                                      ServiceRepository serviceRepository,
                                      ServiceCatalogService serviceCatalogService) {
        this.appointmentRepository = appointmentRepository;
        this.serviceRepository = serviceRepository;
        this.serviceCatalogService = serviceCatalogService;
    }

    @Transactional
    public AppointmentConfirmationView book(BookingRequest request, Locale locale) {
        Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown service id " + request.getServiceId()));

        Appointment appointment = new Appointment(
                request.getClientName(),
                request.getEmail(),
                request.getPhone(),
                service,
                request.getRequestedAt(),
                request.getRequestedAt().plusMinutes(service.getDurationMinutes()),
                request.getNotes()
        );
        Appointment saved = appointmentRepository.save(appointment);
        notifyClient(saved);

        String serviceName = serviceCatalogService.resolveName(service, locale);
        return new AppointmentConfirmationView(saved.getClientName(), serviceName, saved.getRequestedAt());
    }

    private void notifyClient(Appointment appointment) {
        // TODO: send a booking confirmation email once spring-boot-starter-mail/SMTP is configured.
        log.info("Appointment {} created for {} - confirmation email not sent (SMTP not configured)",
                appointment.getId(), appointment.getEmail());
    }
}
