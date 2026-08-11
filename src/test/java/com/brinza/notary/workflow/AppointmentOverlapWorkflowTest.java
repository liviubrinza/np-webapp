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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the "pending appointment overlaps an already-confirmed one" warning (list + detail
 * pages) and the rule that an overlapping appointment cannot be moved into CONFIRMED, which
 * together guarantee CONFIRMED appointments never overlap each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "titi", roles = "TECHNICIAN")
class AppointmentOverlapWorkflowTest {

    private static final String OVERLAP_LIST_MARKER = "Se suprapune cu o programare confirmată";
    private static final String OVERLAP_DETAIL_MARKER = "se suprapune cu o programare deja confirmată";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;

    private Appointment appointmentAt(LocalDateTime start, AppointmentStatus status) {
        Service service = serviceRepository.findByCode("document-authentication").orElseThrow();
        Appointment appointment = new Appointment("Client " + status, "client@example.com", "0700000000", service,
                start, start.plusMinutes(30), null);
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    @Test
    void listMarksPendingAppointmentThatOverlapsAConfirmedOne() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        appointmentAt(start, AppointmentStatus.CONFIRMED);
        appointmentAt(start, AppointmentStatus.PENDING);

        mockMvc.perform(get("/admin/appointments"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(OVERLAP_LIST_MARKER)));
    }

    @Test
    void listDoesNotMarkPendingAppointmentWithNoOverlap() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        appointmentAt(start, AppointmentStatus.CONFIRMED);
        appointmentAt(start.plusHours(2), AppointmentStatus.PENDING);

        mockMvc.perform(get("/admin/appointments"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString(OVERLAP_LIST_MARKER))));
    }

    @Test
    void listDoesNotMarkCancelledAppointmentThatOverlapsAConfirmedOne() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        appointmentAt(start, AppointmentStatus.CONFIRMED);
        appointmentAt(start, AppointmentStatus.CANCELLED);

        mockMvc.perform(get("/admin/appointments"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString(OVERLAP_LIST_MARKER))));
    }

    @Test
    void detailPageDoesNotShowOverlapWarningForCancelledAppointment() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        appointmentAt(start, AppointmentStatus.CONFIRMED);
        Appointment cancelled = appointmentAt(start, AppointmentStatus.CANCELLED);

        mockMvc.perform(get("/admin/appointments/" + cancelled.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString(OVERLAP_DETAIL_MARKER))));
    }

    @Test
    void detailPageShowsOverlapWarningMessage() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        appointmentAt(start, AppointmentStatus.CONFIRMED);
        Appointment pending = appointmentAt(start, AppointmentStatus.PENDING);

        mockMvc.perform(get("/admin/appointments/" + pending.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(OVERLAP_DETAIL_MARKER)));
    }

    @Test
    void confirmingAnOverlappingAppointmentIsRejected() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        appointmentAt(start, AppointmentStatus.CONFIRMED);
        Appointment pending = appointmentAt(start, AppointmentStatus.PENDING);

        mockMvc.perform(post("/admin/appointments/" + pending.getId() + "/status").with(csrf())
                        .param("status", "CONFIRMED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        Appointment reloaded = appointmentRepository.findById(pending.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void confirmingSucceedsOnceTheOverlapIsResolved() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Appointment confirmed = appointmentAt(start, AppointmentStatus.CONFIRMED);
        Appointment pending = appointmentAt(start, AppointmentStatus.PENDING);

        confirmed.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(confirmed);

        mockMvc.perform(post("/admin/appointments/" + pending.getId() + "/status").with(csrf())
                        .param("status", "CONFIRMED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        Appointment reloaded = appointmentRepository.findById(pending.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void reschedulingAConfirmedAppointmentIntoOverlapWithAnotherConfirmedIsRejected() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        appointmentAt(start, AppointmentStatus.CONFIRMED);
        Appointment other = appointmentAt(start.plusHours(2), AppointmentStatus.CONFIRMED);

        mockMvc.perform(post("/admin/appointments/" + other.getId() + "/schedule").with(csrf())
                        .param("date", start.toLocalDate().toString())
                        .param("startTime", start.toLocalTime().toString())
                        .param("endTime", start.plusMinutes(30).toLocalTime().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        Appointment reloaded = appointmentRepository.findById(other.getId()).orElseThrow();
        assertThat(reloaded.getRequestedAt()).isEqualTo(start.plusHours(2));
    }

    @Test
    void reschedulingAPendingAppointmentIntoOverlapWithAConfirmedOneIsRejected() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        appointmentAt(start, AppointmentStatus.CONFIRMED);
        Appointment pending = appointmentAt(start.plusHours(2), AppointmentStatus.PENDING);

        mockMvc.perform(post("/admin/appointments/" + pending.getId() + "/schedule").with(csrf())
                        .param("date", start.toLocalDate().toString())
                        .param("startTime", start.toLocalTime().toString())
                        .param("endTime", start.plusMinutes(30).toLocalTime().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        Appointment reloaded = appointmentRepository.findById(pending.getId()).orElseThrow();
        assertThat(reloaded.getRequestedAt()).isEqualTo(start.plusHours(2));
    }

    @Test
    void busyTimesEndpointReturnsSlotsOccupiedByConfirmedAppointmentsExcludingSelf() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Appointment confirmed = appointmentAt(start, AppointmentStatus.CONFIRMED);
        Appointment pending = appointmentAt(start.plusHours(3), AppointmentStatus.PENDING);

        mockMvc.perform(get("/admin/appointments/" + pending.getId() + "/busy-times")
                        .param("date", start.toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(start.toLocalTime().toString())));

        mockMvc.perform(get("/admin/appointments/" + confirmed.getId() + "/busy-times")
                        .param("date", start.toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"startTimes\":[],\"endTimes\":[]}"));
    }
}
