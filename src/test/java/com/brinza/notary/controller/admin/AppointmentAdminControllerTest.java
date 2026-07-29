package com.brinza.notary.controller.admin;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.config.SystemSettings;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.dto.AppointmentDetailView;
import com.brinza.notary.dto.AppointmentListItemView;
import com.brinza.notary.dto.AppointmentListView;
import com.brinza.notary.service.AdminActivityLogger;
import com.brinza.notary.service.AppointmentManagementService;
import com.brinza.notary.service.DocumentManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AppointmentAdminController.class)
@Import(AdminSessionRegistry.class)
@WithMockUser(roles = "TECHNICIAN")
class AppointmentAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AppointmentManagementService appointmentManagementService;
    @MockitoBean
    private DocumentManagementService documentManagementService;
    @MockitoBean
    private AdminActivityLogger adminActivityLogger;
    @MockitoBean
    private SystemSettings systemSettings;

    @Test
    void listRendersGroupedAppointments() throws Exception {
        when(appointmentManagementService.searchGrouped(any(), any(), any(), any()))
                .thenReturn(new AppointmentListView(List.of(), List.of()));

        mockMvc.perform(get("/admin/appointments"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appointments/list"))
                .andExpect(model().attributeExists("pendingAppointments", "otherAppointments", "statuses"));
    }

    @Test
    void detailRendersMailEnabledFromSystemSettings() throws Exception {
        when(appointmentManagementService.getDetail(1L)).thenReturn(detailView());
        when(documentManagementService.listForAppointment(1L)).thenReturn(List.of());
        when(systemSettings.isMailEnabled()).thenReturn(true);

        mockMvc.perform(get("/admin/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appointments/detail"))
                .andExpect(model().attribute("mailEnabled", true));
    }

    @Test
    void updateStatusRedirectsToDetailWithFlashSuccess() throws Exception {
        mockMvc.perform(post("/admin/appointments/1/status").with(csrf())
                        .param("status", "CONFIRMED")
                        .param("sendConfirmationEmail", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/appointments/1?back=%2Fadmin%2Fappointments"))
                .andExpect(flash().attributeExists("success"));

        verify(appointmentManagementService).updateStatus(eq(1L), eq(AppointmentStatus.CONFIRMED), any(), eq(true));
    }

    @Test
    void updateScheduleRejectsInvalidRangeWithFlashError() throws Exception {
        doThrow(new IllegalArgumentException("Ora de sfarsit trebuie sa fie dupa ora de inceput."))
                .when(appointmentManagementService).updateSchedule(anyLong(), any(), any(), any());

        mockMvc.perform(post("/admin/appointments/1/schedule").with(csrf())
                        .param("date", "2026-08-01")
                        .param("startTime", "10:00")
                        .param("endTime", "09:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void addNoteRejectsBlankNoteWithoutCallingService() throws Exception {
        mockMvc.perform(post("/admin/appointments/1/notes").with(csrf())
                        .param("note", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        verify(appointmentManagementService, org.mockito.Mockito.never()).addInternalNote(anyLong(), any(), any());
    }

    private static AppointmentDetailView detailView() {
        return new AppointmentDetailView(1L, "Ion Popescu", "ion@example.com", "0700000000", "Autentificare",
                LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 9, 30),
                AppointmentStatus.PENDING, "notes", List.of(), LocalDateTime.of(2026, 7, 1, 9, 0));
    }
}
