package com.brinza.notary.controller.web;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.config.SecurityConfig;
import com.brinza.notary.service.GeoLocationService;
import com.brinza.notary.service.TrafficStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeoController.class)
@Import({AdminSessionRegistry.class, SecurityConfig.class, TrafficStatsService.class, GeoLocationService.class})
class SeoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sitemapListsEveryPublicPageInEveryLocale() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(content().string(containsString("<loc>http://localhost:8080/ro</loc>")))
                .andExpect(content().string(containsString("<loc>http://localhost:8080/en/services</loc>")))
                .andExpect(content().string(containsString("<loc>http://localhost:8080/hu/contact</loc>")))
                .andExpect(content().string(containsString("<loc>http://localhost:8080/ro/book</loc>")));
    }

    @Test
    void robotsDisallowsAdminAndH2ConsoleAndPointsToSitemap() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Disallow: /admin/")))
                .andExpect(content().string(containsString("Disallow: /h2-console/")))
                .andExpect(content().string(containsString("Sitemap: http://localhost:8080/sitemap.xml")));
    }
}
