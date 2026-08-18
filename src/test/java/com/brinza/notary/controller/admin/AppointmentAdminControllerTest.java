package com.brinza.notary.controller.admin;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.config.SystemSettings;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.dto.AppointmentDetailView;
import com.brinza.notary.dto.AppointmentListItemView;
import com.brinza.notary.dto.AppointmentListView;
import com.brinza.notary.dto.BusyTimeSlots;
import com.brinza.notary.service.AdminActivityLogger;
import com.brinza.notary.service.AppointmentBookingService;
import com.brinza.notary.service.AppointmentManagementService;
import com.brinza.notary.service.DocumentManagementService;
import com.brinza.notary.service.ServiceCatalogService;
import com.brinza.notary.service.GeoLocationService;
import com.brinza.notary.service.TrafficStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AppointmentAdminController.class)
@Import({AdminSessionRegistry.class, TrafficStatsService.class, GeoLocationService.class})
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
    @MockitoBean
    private ServiceCatalogService serviceCatalogService;
    @MockitoBean
    private AppointmentBookingService appointmentBookingService;

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
    void listBindsMultipleStatusCheckboxesIntoASetAndForwardsToService() throws Exception {
        when(appointmentManagementService.searchGrouped(any(), any(), any(), any()))
                .thenReturn(new AppointmentListView(List.of(), List.of()));

        mockMvc.perform(get("/admin/appointments").param("status", "CONFIRMED", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appointments/list"))
                .andExpect(model().attribute("selectedStatuses", Set.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED)));

        verify(appointmentManagementService).searchGrouped(
                eq(Set.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED)), any(), any(), any());
    }

    @Test
    void detailRendersMailEnabledFromSystemSettings() throws Exception {
        when(appointmentManagementService.getDetail(1L)).thenReturn(detailView());
        when(appointmentManagementService.findBusyTimeSlots(any(), any(), any()))
                .thenReturn(new BusyTimeSlots(Set.of(), Set.of()));
        when(appointmentManagementService.findByDate(any())).thenReturn(List.of());
        when(documentManagementService.listForAppointment(1L)).thenReturn(List.of());
        when(systemSettings.isMailEnabled()).thenReturn(true);

        mockMvc.perform(get("/admin/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appointments/detail"))
                .andExpect(model().attribute("mailEnabled", true));
    }

    @Test
    void detailRendersDayScheduleTimelineWithOtherAppointmentsOnTheSameDate() throws Exception {
        when(appointmentManagementService.getDetail(1L)).thenReturn(detailView());
        when(appointmentManagementService.findBusyTimeSlots(any(), any(), any()))
                .thenReturn(new BusyTimeSlots(Set.of(), Set.of()));
        List<AppointmentListItemView> dayAppointments = List.of(new AppointmentListItemView(
                2L, "Maria Ionescu", "Legalizare", LocalDateTime.of(2026, 8, 1, 11, 0),
                LocalDateTime.of(2026, 8, 1, 11, 30), AppointmentStatus.CONFIRMED, false,
                LocalDateTime.of(2026, 7, 1, 9, 0)));
        when(appointmentManagementService.findByDate(any())).thenReturn(dayAppointments);
        when(documentManagementService.listForAppointment(1L)).thenReturn(List.of());
        when(systemSettings.isMailEnabled()).thenReturn(true);

        mockMvc.perform(get("/admin/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("dayAppointments", dayAppointments))
                .andExpect(content().string(containsString("Maria Ionescu")));

        verify(appointmentManagementService).findByDate(detailView().requestedAt().toLocalDate());
    }

    @Test
    void detailColorCodesStatusDropdownOptionsLikeTheStatusBadgesElsewhere() throws Exception {
        when(appointmentManagementService.getDetail(1L)).thenReturn(detailView());
        when(appointmentManagementService.findBusyTimeSlots(any(), any(), any()))
                .thenReturn(new BusyTimeSlots(Set.of(), Set.of()));
        when(appointmentManagementService.findByDate(any())).thenReturn(List.of());
        when(documentManagementService.listForAppointment(1L)).thenReturn(List.of());
        when(systemSettings.isMailEnabled()).thenReturn(true);

        mockMvc.perform(get("/admin/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"status-option-pending\"")))
                .andExpect(content().string(containsString("class=\"status-option-confirmed\"")))
                .andExpect(content().string(containsString("class=\"status-option-cancelled\"")))
                .andExpect(content().string(containsString("class=\"status-option-completed\"")));
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

    @Test
    void newFormRendersBookingRequestAndServices() throws Exception {
        when(serviceCatalogService.findActiveServices(any())).thenReturn(List.of());
        when(appointmentManagementService.findByDate(any())).thenReturn(List.of());
        when(appointmentManagementService.findBusyTimeSlots(any(), any(), any()))
                .thenReturn(new BusyTimeSlots(Set.of(), Set.of()));

        mockMvc.perform(get("/admin/appointments/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appointments/new"))
                .andExpect(model().attributeExists("bookingRequest", "services", "startTimeSlots", "endTimeSlots",
                        "dayAppointments", "busyTimeSlots"))
                .andExpect(model().attribute("selectedDate", java.time.LocalDate.now()));
    }

    @Test
    void newFormMarksBusyTimeSlotsAsOccupied() throws Exception {
        when(serviceCatalogService.findActiveServices(any())).thenReturn(List.of());
        when(appointmentManagementService.findByDate(any())).thenReturn(List.of());
        when(appointmentManagementService.findBusyTimeSlots(any(), any(), any()))
                .thenReturn(new BusyTimeSlots(Set.of("09:00"), Set.of("09:30")));

        mockMvc.perform(get("/admin/appointments/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("09:00 — ocupat")))
                .andExpect(content().string(containsString("09:30 — ocupat")));
    }

    @Test
    void createAppointmentRedirectsToDetailWithoutSendingReceivedEmail() throws Exception {
        when(appointmentBookingService.bookAsAdmin(any(), any())).thenReturn(7L);

        mockMvc.perform(post("/admin/appointments/new").with(csrf())
                        .param("clientName", "Ion Popescu")
                        .param("email", "ion@example.com")
                        .param("phone", "0700000000")
                        .param("serviceId", "1")
                        .param("requestedAt", futureHalfHourDateTime())
                        .param("endTime", "11:00")
                        .param("notes", "notes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/appointments/7"))
                .andExpect(flash().attributeExists("success"));

        verify(appointmentBookingService).bookAsAdmin(any(), any());
    }

    @Test
    void createAppointmentRerendersFormOnValidationError() throws Exception {
        when(serviceCatalogService.findActiveServices(any())).thenReturn(List.of());
        when(appointmentManagementService.findByDate(any())).thenReturn(List.of());
        when(appointmentManagementService.findBusyTimeSlots(any(), any(), any()))
                .thenReturn(new BusyTimeSlots(Set.of(), Set.of()));

        mockMvc.perform(post("/admin/appointments/new").with(csrf())
                        .param("clientName", "")
                        .param("email", "not-an-email")
                        .param("phone", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appointments/new"));

        verify(appointmentBookingService, org.mockito.Mockito.never()).bookAsAdmin(any(), any());
    }

    @Test
    void createAppointmentRerendersFormOnServiceErrorSuchAsMissingEndTime() throws Exception {
        when(serviceCatalogService.findActiveServices(any())).thenReturn(List.of());
        when(appointmentManagementService.findByDate(any())).thenReturn(List.of());
        when(appointmentManagementService.findBusyTimeSlots(any(), any(), any()))
                .thenReturn(new BusyTimeSlots(Set.of(), Set.of()));
        when(appointmentBookingService.bookAsAdmin(any(), isNull()))
                .thenThrow(new IllegalArgumentException("Ora de sfârșit este obligatorie."));

        mockMvc.perform(post("/admin/appointments/new").with(csrf())
                        .param("clientName", "Ion Popescu")
                        .param("email", "ion@example.com")
                        .param("phone", "0700000000")
                        .param("serviceId", "1")
                        .param("requestedAt", futureHalfHourDateTime())
                        .param("notes", "notes"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appointments/new"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void newBusyTimesReturnsBusyTimeSlotsForGivenDate() throws Exception {
        when(appointmentManagementService.findBusyTimeSlots(any(), isNull(), any()))
                .thenReturn(new BusyTimeSlots(Set.of("09:00"), Set.of("09:30")));

        mockMvc.perform(get("/admin/appointments/new/busy-times").param("date", "2026-08-01"))
                .andExpect(status().isOk());

        verify(appointmentManagementService).findBusyTimeSlots(eq(java.time.LocalDate.of(2026, 8, 1)), isNull(), any());
    }

    @Test
    void newDayScheduleRendersTimelineFragmentForGivenDate() throws Exception {
        when(appointmentManagementService.findByDate(java.time.LocalDate.of(2026, 8, 1))).thenReturn(List.of());

        mockMvc.perform(get("/admin/appointments/new/day-schedule").param("date", "2026-08-01"))
                .andExpect(status().isOk());
    }

    private static String futureHalfHourDateTime() {
        return LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    private static AppointmentDetailView detailView() {
        return new AppointmentDetailView(1L, "Ion Popescu", "ion@example.com", "0700000000", "Autentificare",
                LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 9, 30),
                AppointmentStatus.PENDING, false, "notes", List.of(), LocalDateTime.of(2026, 7, 1, 9, 0));
    }
}
