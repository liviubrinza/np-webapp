package com.brinza.notary.repository;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.domain.Service;
import com.brinza.notary.migration.V11__AddAppointmentEndedAt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest's restricted component scan does not pick up our one Java-based Flyway
// migration (a plain @Component); without this @Import, Flyway silently skips it and the
// appointments.ended_at column never gets created.
@DataJpaTest
@Import(V11__AddAppointmentEndedAt.class)
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private ServiceRepository serviceRepository;

    private Service service;

    @BeforeEach
    void setUp() {
        service = new Service(30, true);
        service.setCode("test-service");
        service = serviceRepository.save(service);
    }

    private Appointment appointmentWith(String clientName, AppointmentStatus status, LocalDateTime requestedAt) {
        Appointment appointment = new Appointment(clientName, clientName + "@example.com", "0700000000", service,
                requestedAt, requestedAt.plusMinutes(30), null);
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    @Test
    void prePersistSetsCreatedAt() {
        Appointment saved = appointmentWith("Ion", AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void searchFiltersByStatus() {
        appointmentWith("Ion", AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        appointmentWith("Maria", AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 2, 9, 0));

        List<Appointment> pending = appointmentRepository.search(AppointmentStatus.PENDING, null, null, null);

        assertThat(pending).extracting(Appointment::getClientName).containsExactly("Ion");
    }

    @Test
    void searchFiltersByDateRange() {
        appointmentWith("Early", AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        appointmentWith("Late", AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 10, 9, 0));

        List<Appointment> result = appointmentRepository.search(null,
                LocalDateTime.of(2026, 8, 5, 0, 0), LocalDateTime.of(2026, 8, 15, 0, 0), null);

        assertThat(result).extracting(Appointment::getClientName).containsExactly("Late");
    }

    @Test
    void searchFiltersByNameCaseInsensitiveContains() {
        appointmentWith("Ion Popescu", AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        appointmentWith("Maria Ionescu", AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 2, 9, 0));

        List<Appointment> result = appointmentRepository.search(null, null, null, "ion");

        assertThat(result).hasSize(2);
    }

    @Test
    void searchWithAllNullFiltersReturnsEverythingNewestFirst() {
        appointmentWith("First", AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        appointmentWith("Second", AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 2, 9, 0));

        List<Appointment> result = appointmentRepository.search(null, null, null, null);

        assertThat(result).extracting(Appointment::getClientName).containsExactly("Second", "First");
    }

    @Test
    void findAllByCreatedAtRangeRespectsBounds() {
        Appointment a = appointmentWith("A", AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        appointmentWith("B", AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 2, 9, 0));

        List<Appointment> all = appointmentRepository.findAllByCreatedAtRange(null, null);
        assertThat(all).hasSize(2);

        List<Appointment> none = appointmentRepository.findAllByCreatedAtRange(a.getCreatedAt().plusYears(1), null);
        assertThat(none).isEmpty();
    }
}
