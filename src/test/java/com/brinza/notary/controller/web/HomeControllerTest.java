package com.brinza.notary.controller.web;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.config.ContactConfig;
import com.brinza.notary.config.SecurityConfig;
import com.brinza.notary.service.StructuredDataService;
import com.brinza.notary.service.GeoLocationService;
import com.brinza.notary.service.TrafficStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// AdminSessionRegistry: see AdminHomeControllerTest - @WebMvcTest registers the global
// AdminSessionCorrelationFilter regardless of which controller is sliced.
// SecurityConfig: without the real filter chain, Boot's default @WebMvcTest security
// auto-config denies every request; the real config's permitAll for non-/admin paths is
// exactly the behavior these public-site tests need to exercise.
@WebMvcTest(HomeController.class)
@Import({AdminSessionRegistry.class, SecurityConfig.class, ContactConfig.class, StructuredDataService.class, TrafficStatsService.class, GeoLocationService.class})
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

    @Test
    void romanianHomeIncludesCanonicalHreflangAndMetaDescription() throws Exception {
        mockMvc.perform(get("/ro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<link rel=\"canonical\" href=\"http://localhost:8080/ro\"")))
                .andExpect(content().string(containsString("hreflang=\"ro\" href=\"http://localhost:8080/ro\"")))
                .andExpect(content().string(containsString("hreflang=\"en\" href=\"http://localhost:8080/en\"")))
                .andExpect(content().string(containsString("hreflang=\"hu\" href=\"http://localhost:8080/hu\"")))
                .andExpect(content().string(containsString("hreflang=\"x-default\" href=\"http://localhost:8080/ro\"")))
                .andExpect(content().string(containsString("<meta name=\"description\"")))
                .andExpect(content().string(not(containsString("name=\"robots\" content=\"noindex,nofollow\""))));
    }

    @Test
    void homePageHasNoBreadcrumbStructuredData() throws Exception {
        mockMvc.perform(get("/ro"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("BreadcrumbList"))));
    }

    @Test
    void homePageFooterIncludesNapBlock() throws Exception {
        mockMvc.perform(get("/ro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Test Address 1, Test City, County, 111111")))
                .andExpect(content().string(containsString("href=\"tel:0700000000\"")))
                .andExpect(content().string(containsString("href=\"mailto:test@example.com\"")));
    }
}
