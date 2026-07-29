package com.brinza.notary.controller.web;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// AdminSessionRegistry: see AdminHomeControllerTest - @WebMvcTest registers the global
// AdminSessionCorrelationFilter regardless of which controller is sliced.
// SecurityConfig: without the real filter chain, Boot's default @WebMvcTest security
// auto-config denies every request; the real config's permitAll for non-/admin paths is
// exactly the behavior these public-site tests need to exercise.
@WebMvcTest(HomeController.class)
@Import({AdminSessionRegistry.class, SecurityConfig.class})
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void romanianRootRendersHomeView() throws Exception {
        mockMvc.perform(get("/ro"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/home"))
                .andExpect(model().attribute("currentLocale", "ro"));
    }

    @Test
    void englishRootWithTrailingSlashRendersHomeView() throws Exception {
        mockMvc.perform(get("/en/"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/home"))
                .andExpect(model().attribute("currentLocale", "en"));
    }
}
