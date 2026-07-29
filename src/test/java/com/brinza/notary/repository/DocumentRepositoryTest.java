package com.brinza.notary.repository;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.domain.Document;
import com.brinza.notary.domain.Service;
import com.brinza.notary.migration.V11__AddAppointmentEndedAt;
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
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private ServiceRepository serviceRepository;

    @Test
    void prePersistSetsUploadedAt() {
        Appointment appointment = appointment();
        Document document = new Document("t", null, "path", "file.pdf", "application/pdf", appointment);

        Document saved = documentRepository.save(document);

        assertThat(saved.getUploadedAt()).isNotNull();
    }

    @Test
    void findByAppointmentIdOrdersByUploadedAtAscending() throws Exception {
        // uploaded_at is @Column(updatable = false), so it can only be controlled by real
        // insertion order/timing, not by mutating an already-persisted entity - hence the sleep
        // to guarantee the two @PrePersist timestamps are distinct.
        Appointment appointment = appointment();
        Document first = documentRepository.save(new Document("t1", null, "path1", "a.pdf", "application/pdf", appointment));
        Thread.sleep(5);
        Document second = documentRepository.save(new Document("t2", null, "path2", "b.pdf", "application/pdf", appointment));

        List<Document> result = documentRepository.findByAppointmentIdOrderByUploadedAtAsc(appointment.getId());

        assertThat(result).extracting(Document::getOriginalFilename).containsExactly("a.pdf", "b.pdf");
        assertThat(first.getUploadedAt()).isBefore(second.getUploadedAt());
    }

    private Appointment appointment() {
        Service service = new Service(30, true);
        service.setCode("code-" + System.nanoTime());
        service = serviceRepository.save(service);
        Appointment appointment = new Appointment("Client", "c@example.com", "0700000000", service,
                LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 9, 30), null);
        appointment.setStatus(AppointmentStatus.PENDING);
        return appointmentRepository.save(appointment);
    }
}
