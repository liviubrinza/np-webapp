package com.brinza.notary.workflow;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.domain.Service;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * End-to-end coverage of the public booking flow named in CLAUDE.md's testing goals: submit ->
 * appointment persisted PENDING -> confirmation page. Exercises the real Flyway-migrated schema,
 * the real seeded services (services.yml), and the real validation chain - not mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingWorkflowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void validSubmissionPersistsPendingAppointmentAndRedirectsToConfirmation() throws Exception {
        Service service = serviceRepository.findByCode("document-authentication").orElseThrow();

        mockMvc.perform(post("/ro/book").with(csrf())
                        .param("clientName", "Workflow Test Client")
                        .param("email", "workflow-test-client@example.com")
                        .param("phone", "0700000000")
                        .param("serviceId", service.getId().toString())
                        .param("requestedAt", nextValidSlot())
                        .param("notes", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ro/book/confirmation"));

        List<Appointment> matches = appointmentRepository.findAll().stream()
                .filter(a -> a.getEmail().equals("workflow-test-client@example.com"))
                .toList();
        assertThat(matches).hasSize(1);
        Appointment saved = matches.get(0);
        assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(saved.getEndedAt()).isEqualTo(saved.getRequestedAt().plusMinutes(service.getDurationMinutes()));
    }

    @Test
    void invalidSubmissionRedisplaysFormAndPersistsNothing() throws Exception {
        long before = appointmentRepository.count();

        mockMvc.perform(post("/ro/book").with(csrf())
                        .param("clientName", "")
                        .param("email", "not-an-email")
                        .param("phone", "0700000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/book"));

        assertThat(appointmentRepository.count()).isEqualTo(before);
    }

    /** Two weeks out at 10:00 - safely in the future and on a valid half-hour booking slot. */
    private static String nextValidSlot() {
        return LocalDate.now().plusDays(14) + "T10:00";
    }
}
