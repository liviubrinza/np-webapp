package com.brinza.notary.controller.web;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.config.ContactConfig;
import com.brinza.notary.config.SecurityConfig;
import com.brinza.notary.service.StructuredDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ContactController.class)
@Import({AdminSessionRegistry.class, SecurityConfig.class, ContactConfig.class, StructuredDataService.class})
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rendersContactDetailsFromConfiguredProperties() throws Exception {
        mockMvc.perform(get("/ro/contact"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/contact"))
                .andExpect(model().attributeExists("address", "phone", "email", "hours", "latitude", "longitude"))
                .andExpect(model().attribute("address", "Test Address 1, Test City 111111"))
                .andExpect(model().attribute("hours", "09:00 - 17:00"));
    }

    @Test
    void includesLegalServiceStructuredData() throws Exception {
        mockMvc.perform(get("/ro/contact"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"@type\":\"LegalService\"")))
                .andExpect(content().string(containsString("\"@type\":\"PostalAddress\"")))
                .andExpect(content().string(containsString("\"@type\":\"OpeningHoursSpecification\"")))
                .andExpect(content().string(containsString("\"telephone\":\"0700000000\"")));
    }

    @Test
    void phoneAndEmailAreClickableLinks() throws Exception {
        mockMvc.perform(get("/ro/contact"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"tel:0700000000\"")))
                .andExpect(content().string(containsString("href=\"mailto:test@example.com\"")));
    }
}
