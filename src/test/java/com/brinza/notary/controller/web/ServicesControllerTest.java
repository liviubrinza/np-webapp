package com.brinza.notary.controller.web;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.config.ContactConfig;
import com.brinza.notary.config.SecurityConfig;
import com.brinza.notary.dto.ServiceView;
import com.brinza.notary.service.ServiceCatalogService;
import com.brinza.notary.service.StructuredDataService;
import com.brinza.notary.service.GeoLocationService;
import com.brinza.notary.service.TrafficStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ServicesController.class)
@Import({AdminSessionRegistry.class, SecurityConfig.class, ContactConfig.class, StructuredDataService.class, TrafficStatsService.class, GeoLocationService.class})
class ServicesControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ServiceCatalogService serviceCatalogService;

    @Test
    void rendersActiveServicesForCurrentLocale() throws Exception {
        when(serviceCatalogService.findActiveServices(any())).thenReturn(
                List.of(new ServiceView(1L, "Autentificare", "descriere", 30)));

        mockMvc.perform(get("/ro/services"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/services"))
                .andExpect(model().attribute("services",
                        List.of(new ServiceView(1L, "Autentificare", "descriere", 30))));
    }

    @Test
    void includesServiceStructuredData() throws Exception {
        when(serviceCatalogService.findActiveServices(any())).thenReturn(
                List.of(new ServiceView(1L, "Autentificare", "descriere", 30)));

        mockMvc.perform(get("/ro/services"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"@type\":\"Service\"")))
                .andExpect(content().string(containsString("\"name\":\"Autentificare\"")))
                .andExpect(content().string(containsString("\"@type\":\"LegalService\"")));
    }

    @Test
    void showsTheDurationDisclaimerInEveryLocale() throws Exception {
        when(serviceCatalogService.findActiveServices(any())).thenReturn(
                List.of(new ServiceView(1L, "Autentificare", "descriere", 30)));

        // One assertion per locale: a key missing from one messages file renders as
        // ??services.duration.disclaimer_xx?? rather than failing, so only the translated text
        // proves the translation is actually there.
        mockMvc.perform(get("/ro/services"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<strong>Precizare:</strong>")))
                .andExpect(content().string(containsString("Durata afișată pentru fiecare serviciu este estimativă")));
        mockMvc.perform(get("/en/services"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<strong>Disclaimer:</strong>")))
                .andExpect(content().string(containsString("The duration shown for each service is an estimate")));
        mockMvc.perform(get("/hu/services"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<strong>Tájékoztatás:</strong>")))
                .andExpect(content().string(containsString("feltüntetett időtartam becsült érték")));
    }

    @Test
    void linksToBookingForm() throws Exception {
        when(serviceCatalogService.findActiveServices(any())).thenReturn(List.of());

        mockMvc.perform(get("/ro/services"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/ro/book\"")));
    }
}
