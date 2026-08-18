package com.brinza.notary.controller.admin;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.dto.AppointmentMonthlyStatsView;
import com.brinza.notary.dto.ClientTrafficView;
import com.brinza.notary.dto.PageTimeView;
import com.brinza.notary.service.AppointmentManagementService;
import com.brinza.notary.service.LogViewerService;
import com.brinza.notary.service.PublicPage;
import com.brinza.notary.service.TrafficStatsService;
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
    @MockitoBean
    private TrafficStatsService trafficStatsService;

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
    void trafficRendersClientTrafficSnapshotWithTotalsAndAverage() throws Exception {
        List<ClientTrafficView> snapshot = List.of(
                new ClientTrafficView("203.0.113.5", java.time.Duration.ofMinutes(7), 2, "Cluj-Napoca, Romania",
                        List.of(new PageTimeView(PublicPage.SERVICES, java.time.Duration.ofMinutes(7)))),
                new ClientTrafficView("198.51.100.9", java.time.Duration.ofMinutes(3), 1, null,
                        List.of(new PageTimeView(PublicPage.HOME, java.time.Duration.ofMinutes(3)))));
        when(trafficStatsService.snapshot()).thenReturn(snapshot);

        mockMvc.perform(get("/admin/statistics/traffic"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/statistics/traffic"))
                .andExpect(model().attribute("clientTraffic", snapshot))
                .andExpect(model().attribute("totalClients", 2))
                .andExpect(model().attribute("averageTime", java.time.Duration.ofMinutes(5)));
    }

    @Test
    void trafficWithNoClientsHasZeroTotalsAndAverage() throws Exception {
        when(trafficStatsService.snapshot()).thenReturn(List.of());

        mockMvc.perform(get("/admin/statistics/traffic"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/statistics/traffic"))
                .andExpect(model().attribute("totalClients", 0))
                .andExpect(model().attribute("averageTime", java.time.Duration.ZERO));
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
