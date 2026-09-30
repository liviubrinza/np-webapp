package com.brinza.notary.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every calendar in the app is configured in one place - {@code static/js/datepicker.js}, pulled in
 * by the {@code fragments/datepicker} fragments - so that pickers added later keep the same look
 * and behaviour (Monday-first week above all) without each page repeating the options.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SharedDatePickerConfigurationTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Path SHARED_FRAGMENT = TEMPLATES.resolve("fragments/datepicker.html");
    private static final Pattern VERSIONED_SCRIPT = Pattern.compile("/js/datepicker-([0-9a-f]{32})\\.js");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theSharedConfigurationStartsEveryWeekOnMonday() throws IOException {
        assertThat(Files.readString(Path.of("src/main/resources/static/js/datepicker.js")))
                .contains("firstDayOfWeek = 1");
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void theVacationRangePickersGoThroughTheSharedConfiguration() throws Exception {
        String html = mockMvc.perform(get("/admin/settings/notification"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Native date inputs take their first day of week from the browser, not from our config.
        assertThat(html).as("no native date input in %s", html).doesNotContain("type=\"date\"");
        assertThat(html).containsPattern(VERSIONED_SCRIPT);
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void theSharedConfigurationIsServedUnderItsContentHash() throws Exception {
        String html = mockMvc.perform(get("/admin/settings/notification"))
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = VERSIONED_SCRIPT.matcher(html);
        assertThat(matcher.find()).as("versioned script link in %s", html).isTrue();

        mockMvc.perform(get(matcher.group()))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("flatpickr.setDefaults"));
    }

    @Test
    void thePublicPickerLoadsTheSharedConfigurationAfterItsLocaleBundle() throws Exception {
        String html = mockMvc.perform(get("/ro/book"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // The Romanian bundle carries its own firstDayOfWeek, so the shared file has to win.
        assertThat(html.indexOf("l10n/ro.js")).isGreaterThan(0);
        Matcher matcher = VERSIONED_SCRIPT.matcher(html);
        assertThat(matcher.find()).as("versioned script link in %s", html).isTrue();
        assertThat(matcher.start()).isGreaterThan(html.indexOf("l10n/ro.js"));
    }

    @Test
    void everyTemplateWithACalendarIncludesTheSharedFragments() throws IOException {
        for (Path template : templatesCreatingAPicker()) {
            String source = Files.readString(template);
            assertThat(source).as("%s includes the shared picker scripts", template)
                    .contains("~{fragments/datepicker :: scripts}");
            assertThat(source).as("%s includes the shared picker styles", template)
                    .contains("~{fragments/datepicker :: styles}");
        }
    }

    @Test
    void noTemplateRepeatsTheSharedConfiguration() throws IOException {
        for (Path template : templatesCreatingAPicker()) {
            assertThat(Files.readString(template))
                    .as("%s leaves the shared options to fragments/datepicker.html", template)
                    .doesNotContain("firstDayOfWeek")
                    .doesNotContain("dateFormat")
                    .doesNotContain("altFormat")
                    .doesNotContain("cdn.jsdelivr.net/npm/flatpickr");
        }
    }

    private static List<Path> templatesCreatingAPicker() throws IOException {
        try (Stream<Path> files = Files.walk(TEMPLATES)) {
            List<Path> templates = files.filter(Files::isRegularFile)
                    .filter(path -> !path.equals(SHARED_FRAGMENT))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains("flatpickr(");
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .toList();
            assertThat(templates).as("templates creating a date picker").isNotEmpty();
            return templates;
        }
    }
}
