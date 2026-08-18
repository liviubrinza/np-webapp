package com.brinza.notary.config;

import com.brinza.notary.controller.web.HomeController;
import com.brinza.notary.service.GeoLocationService;
import com.brinza.notary.service.TrafficStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
@Import({AdminSessionRegistry.class, SecurityConfig.class, TrafficStatsService.class, GeoLocationService.class})
class WebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootRedirectsPermanentlyToDefaultLocale() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("/ro"));
    }
}
