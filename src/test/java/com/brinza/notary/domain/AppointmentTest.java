package com.brinza.notary.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentTest {

    private final Service service = new Service(30, true);

    @Test
    void constructorDefaultsStatusToPending() {
        Appointment appointment = new Appointment(
                "Client", "client@example.com", "0700000000", service,
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 1, 10, 30),
                "notes");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void addInternalNoteSetsBackReferenceAndAppends() {
        Appointment appointment = new Appointment(
                "Client", "client@example.com", "0700000000", service,
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 1, 10, 30),
                "notes");
        InternalNote note = new InternalNote("titi", "note text");

        appointment.addInternalNote(note);

        assertThat(appointment.getInternalNotes()).containsExactly(note);
        assertThat(note.getAppointment()).isSameAs(appointment);
    }
}
