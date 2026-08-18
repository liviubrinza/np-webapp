package com.brinza.notary.controller.admin;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.dto.DayAvailability;
import com.brinza.notary.service.AppointmentManagementService;
import com.brinza.notary.service.GeoLocationService;
import com.brinza.notary.service.TrafficStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CalendarAdminController.class)
@Import({AdminSessionRegistry.class, TrafficStatsService.class, GeoLocationService.class})
@WithMockUser(roles = "TECHNICIAN")
class CalendarAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AppointmentManagementService appointmentManagementService;

    @Test
    void viewWithoutDateDefaultsToToday() throws Exception {
        when(appointmentManagementService.monthAvailability(any())).thenReturn(Map.of());
        when(appointmentManagementService.findByDate(any())).thenReturn(java.util.List.of());

        mockMvc.perform(get("/admin/calendar"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/calendar"))
                .andExpect(model().attribute("selectedDate", LocalDate.now()));
    }

    @Test
    void viewWithExplicitDateUsesIt() throws Exception {
        when(appointmentManagementService.monthAvailability(any())).thenReturn(
                Map.of(LocalDate.of(2026, 8, 1), DayAvailability.FULL));
        when(appointmentManagementService.findByDate(LocalDate.of(2026, 8, 1))).thenReturn(java.util.List.of());

        mockMvc.perform(get("/admin/calendar").param("date", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedDate", LocalDate.of(2026, 8, 1)))
                .andExpect(model().attribute("dayAvailabilityJson", "{\"2026-08-01\":\"FULL\"}"));
    }
}
