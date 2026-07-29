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

@WebMvcTest(ContactController.class)
@Import({AdminSessionRegistry.class, SecurityConfig.class})
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rendersContactDetailsFromConfiguredProperties() throws Exception {
        mockMvc.perform(get("/ro/contact"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/contact"))
                .andExpect(model().attributeExists("address", "phone", "email", "hours", "latitude", "longitude"));
    }
}
