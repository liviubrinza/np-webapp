package com.brinza.notary.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Static assets are cached for a week ({@link WebConfig}), so their URLs must carry a content
 * hash — otherwise an edited stylesheet cannot reach a browser holding the previous copy.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StaticAssetVersioningTest {

    private static final Pattern VERSIONED_CSS = Pattern.compile("/css/style-([0-9a-f]{32})\\.css");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void templatesLinkTheStylesheetUnderItsContentHash() throws Exception {
        String html = mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Matcher matcher = VERSIONED_CSS.matcher(html);
        assertThat(matcher.find()).as("versioned stylesheet link in %s", html).isTrue();
        assertThat(html).doesNotContain("\"/css/style.css\"");
    }

    @Test
    void theVersionedUrlServesTheStylesheetWithItsLongLivedCacheHeader() throws Exception {
        String html = mockMvc.perform(get("/admin/login")).andReturn().getResponse().getContentAsString();
        Matcher matcher = VERSIONED_CSS.matcher(html);
        assertThat(matcher.find()).isTrue();

        mockMvc.perform(get(matcher.group()))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getHeader("Cache-Control"))
                        .isEqualTo("max-age=604800, public"));
    }
}
