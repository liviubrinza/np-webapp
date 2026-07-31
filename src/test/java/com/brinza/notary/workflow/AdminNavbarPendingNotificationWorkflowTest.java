package com.brinza.notary.workflow;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.domain.Service;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.ServiceRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the admin navbar's "Programări" notification mark: visible while at least one
 * PENDING appointment exists, gone the moment none do.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "titi", roles = "TECHNICIAN")
class AdminNavbarPendingNotificationWorkflowTest {

    private static final String NOTIFICATION_MARKER = "Există programări în așteptare";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;

    private Appointment appointmentWith(AppointmentStatus status) {
        Service service = serviceRepository.findByCode("document-authentication").orElseThrow();
        LocalDateTime requestedAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Appointment appointment = new Appointment("Client", "client@example.com", "0700000000", service,
                requestedAt, requestedAt.plusMinutes(30), null);
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    @Test
    void noNotificationWhenNoPendingAppointmentsExist() throws Exception {
        appointmentWith(AppointmentStatus.CONFIRMED);

        mockMvc.perform(get("/admin/appointments"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString(NOTIFICATION_MARKER))));
    }

    @Test
    void notificationShownWhilePendingAppointmentExists() throws Exception {
        appointmentWith(AppointmentStatus.PENDING);

        mockMvc.perform(get("/admin/appointments"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(NOTIFICATION_MARKER)));
    }

    @Test
    void notificationDisappearsAfterLastPendingAppointmentIsResolved() throws Exception {
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appointment);

        mockMvc.perform(get("/admin/appointments"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString(NOTIFICATION_MARKER))));
    }
}
