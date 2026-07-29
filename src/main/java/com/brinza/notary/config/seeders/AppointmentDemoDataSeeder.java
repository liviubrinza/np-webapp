package com.brinza.notary.config.seeders;

import com.brinza.notary.config.properties.AppointmentSeedProperties;
import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.Service;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads the fixed demo/test appointments from {@code appointments.yml} on startup when
 * {@code app.demo-data.appointments.enabled} is true. Disabled by default, and skipped if
 * the table already has rows, since this is one-off demo data, not a live source of truth
 * like {@link ServiceSeeder}/{@link AdminUserSeeder} - re-running it on every restart would
 * keep piling on duplicates. Every appointment detail (including status and created-at) comes
 * straight from {@code appointments.yml}; {@code bookedService} only resolves the linked
 * {@link Service} by its stable code, it does not drive the appointment's duration.
 */
@Component
@Order(3)
public class AppointmentDemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AppointmentDemoDataSeeder.class);

    private final boolean enabled;
    private final AppointmentSeedProperties properties;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;

    public AppointmentDemoDataSeeder(@Value("${app.demo-data.appointments.enabled:false}") boolean enabled,
                                      AppointmentSeedProperties properties,
                                      AppointmentRepository appointmentRepository,
                                      ServiceRepository serviceRepository) {
        this.enabled = enabled;
        this.properties = properties;
        this.appointmentRepository = appointmentRepository;
        this.serviceRepository = serviceRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            log.debug("Demo appointment loading disabled via app.demo-data.appointments.enabled");
            return;
        }
        if (appointmentRepository.count() > 0) {
            log.debug("Skipping demo appointment loading - appointments table is not empty.");
            return;
        }

        List<Appointment> appointments = new ArrayList<>();
        for (AppointmentSeedProperties.AppointmentDefinition definition : properties.demoAppointments()) {
            Service service = serviceRepository.findByCode(definition.bookedService())
                    .orElseThrow(() -> new IllegalStateException(
                            "Unknown service code in appointments.yml: " + definition.bookedService()));

            Appointment appointment = new Appointment(
                    definition.clientName(),
                    definition.email(),
                    definition.phone(),
                    service,
                    definition.requestedAt(),
                    definition.requestedAt().plusMinutes(definition.duration()),
                    definition.notes());
            appointment.setStatus(definition.status());
            appointment.setCreatedAt(definition.bookedDate());
            appointments.add(appointment);
        }

        appointmentRepository.saveAll(appointments);
        log.debug("Loaded {} demo appointments from appointments.yml.", appointments.size());
    }
}
