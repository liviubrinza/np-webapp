package com.brinza.notary.controller.admin;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.dto.AppointmentMonthlyStatsView;
import com.brinza.notary.service.AppointmentManagementService;
import com.brinza.notary.service.LogViewerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StatisticsAdminController.class)
@Import(AdminSessionRegistry.class)
@WithMockUser(roles = "TECHNICIAN")
class StatisticsAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AppointmentManagementService appointmentManagementService;
    @MockitoBean
    private LogViewerService logViewerService;

    @Test
    void requestsRendersMonthlyStatsAndChartJson() throws Exception {
        when(appointmentManagementService.monthlyStatusSummary(any(), any())).thenReturn(
                List.of(new AppointmentMonthlyStatsView(LocalDate.of(2026, 7, 1), 1, 2, 0, 3, 6)));

        mockMvc.perform(get("/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/statistics/requests"))
                .andExpect(model().attributeExists("monthlyStats", "monthlyStatsChartJson"));
    }

    @Test
    void trafficRendersStaticView() throws Exception {
        mockMvc.perform(get("/admin/statistics/traffic"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/statistics/traffic"));
    }

    @Test
    void activityWithNoAvailableDatesRendersEmptyEntries() throws Exception {
        when(logViewerService.availableDates()).thenReturn(List.of());

        mockMvc.perform(get("/admin/statistics/activity"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/statistics/activity"))
                .andExpect(model().attribute("selectedDate", (Object) null))
                .andExpect(model().attribute("entries", List.of()));
    }

    @Test
    void logsUsesFirstAvailableDateWhenNoneSpecified() throws Exception {
        LocalDate mostRecent = LocalDate.of(2026, 7, 29);
        when(logViewerService.availableDates()).thenReturn(List.of(mostRecent));
        when(logViewerService.readEntries(mostRecent, null, null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/admin/statistics/logs"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/statistics/logs"))
                .andExpect(model().attribute("selectedDate", mostRecent))
                .andExpect(model().attributeExists("levels"));
    }
}
