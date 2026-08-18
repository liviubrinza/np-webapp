package com.brinza.notary.controller.admin;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.service.GeoLocationService;
import com.brinza.notary.service.TrafficStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest registers Filter-type @Component beans (unlike plain @Components), so the global
// AdminSessionCorrelationFilter and PublicTrafficTrackingFilter get pulled into the slice; they
// need AdminSessionRegistry and TrafficStatsService respectively, neither of which is otherwise
// part of this slice - every admin-controller @WebMvcTest needs this same import.
@WebMvcTest(AdminHomeController.class)
@Import({AdminSessionRegistry.class, TrafficStatsService.class, GeoLocationService.class})
class AdminHomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void technicianIsRedirectedToStatistics() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/statistics"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminIsRedirectedToAppointments() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/appointments"));
    }
}
