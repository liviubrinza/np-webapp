package com.brinza.notary.workflow;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.Service;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.ServiceRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression coverage for a bug fixed this session: the appointment status-change page's
 * "send confirmation email?" popup, and the actual send attempt, must both be gated by the
 * live {@link com.brinza.notary.config.SystemSettings} flag - not the static
 * {@code app.mail.enabled} default that {@code AppointmentAdminController} used to read.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "titi", roles = "TECHNICIAN")
class AdminAppointmentStatusWorkflowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @MockitoBean
    private JavaMailSender mailSender;

    private Appointment pendingAppointment() {
        Service service = serviceRepository.findByCode("document-authentication").orElseThrow();
        LocalDateTime requestedAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Appointment appointment = new Appointment("Client", "client@example.com", "0700000000", service,
                requestedAt, requestedAt.plusMinutes(30), null);
        return appointmentRepository.save(appointment);
    }

    @Test
    void detailPageReflectsMailDisabledByDefault() throws Exception {
        Appointment appointment = pendingAppointment();

        mockMvc.perform(get("/admin/appointments/" + appointment.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("data-mail-enabled=\"false\"")));
    }

    @Test
    void detailPageReflectsMailEnabledAfterToggling() throws Exception {
        mockMvc.perform(post("/admin/settings/mail-enabled").with(csrf()).param("enabled", "true"))
                .andExpect(status().is3xxRedirection());
        try {
            Appointment appointment = pendingAppointment();

            mockMvc.perform(get("/admin/appointments/" + appointment.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("data-mail-enabled=\"true\"")));
        } finally {
            // The SystemSettings in-memory cache is a singleton field, not transaction-scoped -
            // reset it explicitly so it doesn't leak into other tests sharing this context.
            mockMvc.perform(post("/admin/settings/mail-enabled").with(csrf()));
        }
    }

    @Test
    void confirmingWithMailDisabledNeverAttemptsToSend() throws Exception {
        Appointment appointment = pendingAppointment();

        mockMvc.perform(post("/admin/appointments/" + appointment.getId() + "/status").with(csrf())
                        .param("status", "CONFIRMED")
                        .param("sendConfirmationEmail", "true"))
                .andExpect(status().is3xxRedirection());

        Thread.sleep(300);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void confirmingWithMailEnabledAndFlagTrueAttemptsToSend() throws Exception {
        mockMvc.perform(post("/admin/settings/mail-enabled").with(csrf()).param("enabled", "true"))
                .andExpect(status().is3xxRedirection());
        try {
            Appointment appointment = pendingAppointment();

            mockMvc.perform(post("/admin/appointments/" + appointment.getId() + "/status").with(csrf())
                            .param("status", "CONFIRMED")
                            .param("sendConfirmationEmail", "true"))
                    .andExpect(status().is3xxRedirection());

            verify(mailSender, timeout(2000)).send(any(SimpleMailMessage.class));
        } finally {
            mockMvc.perform(post("/admin/settings/mail-enabled").with(csrf()));
        }
    }
}
