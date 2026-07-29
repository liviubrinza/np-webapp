package com.brinza.notary.controller.web;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.config.SecurityConfig;
import com.brinza.notary.dto.ServiceView;
import com.brinza.notary.service.ServiceCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ServicesController.class)
@Import({AdminSessionRegistry.class, SecurityConfig.class})
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
}
