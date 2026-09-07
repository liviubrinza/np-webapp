package com.brinza.notary.workflow;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the admin-configurable maintenance/notice banner shown on public pages: hidden by
 * default, visible with the configured message once an ADMIN or TECHNICIAN turns it on via the
 * Configurare page's Notificare panel, gone again once turned off. Also confirms both roles can
 * reach the Notificare panel itself (the rest of the Configurare page stays TECHNICIAN-only, see
 * {@link SecurityAccessWorkflowTest}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicNotificationBannerWorkflowTest {

    private static final String MESSAGE = "Biroul este închis pentru inventar pe 30 august.";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "titi", roles = "TECHNICIAN")
    void bannerHiddenByDefault() throws Exception {
        mockMvc.perform(get("/ro"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString(MESSAGE))));
    }

    @Test
    @WithMockUser(username = "titi", roles = "ADMIN")
    void adminCanEnableBannerAndItShowsOnPublicPages() throws Exception {
        mockMvc.perform(post("/admin/settings/notification").with(csrf())
                        .param("enabled", "true")
                        .param("message", MESSAGE))
                .andExpect(status().is3xxRedirection());
        try {
            mockMvc.perform(get("/ro"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString(MESSAGE)));
            mockMvc.perform(get("/ro/services"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString(MESSAGE)));
        } finally {
            // The SystemSettings in-memory cache is a singleton field, not transaction-scoped -
            // reset it explicitly so it doesn't leak into other tests sharing this context.
            mockMvc.perform(post("/admin/settings/notification").with(csrf()));
        }
    }

    @Test
    @WithMockUser(username = "titi", roles = "TECHNICIAN")
    void bannerDisappearsAfterBeingTurnedOff() throws Exception {
        mockMvc.perform(post("/admin/settings/notification").with(csrf())
                        .param("enabled", "true")
                        .param("message", MESSAGE))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/settings/notification").with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/ro"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString(MESSAGE))));
    }

    // Vacation-range-only scenario (no custom message typed): the banner falls back to the
    // translated "office is closed" template, rendered in whichever locale the visitor is on,
    // with the start/end dates individually bolded rather than flowing as plain text.
    @Test
    @WithMockUser(username = "titi", roles = "TECHNICIAN")
    void vacationRangeWithoutMessageShowsTranslatedTemplateInEachLocale() throws Exception {
        mockMvc.perform(post("/admin/settings/notification").with(csrf())
                        .param("enabled", "true")
                        .param("vacationStart", "2026-08-01")
                        .param("vacationEnd", "2026-08-15"))
                .andExpect(status().is3xxRedirection());
        try {
            mockMvc.perform(get("/en"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("Our office is closed between")))
                    .andExpect(content().string(Matchers.containsString("<strong>01 August 2026</strong>")))
                    .andExpect(content().string(Matchers.containsString("<strong>15 August 2026</strong>")))
                    .andExpect(content().string(Matchers.containsString(
                            "No appointments can be made during this time. We will process other "
                                    + "appointment requests as soon as we return. Thank you for your understanding.")));
            mockMvc.perform(get("/ro"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("Biroul nostru este închis în perioada")))
                    .andExpect(content().string(Matchers.containsString("<strong>01 august 2026</strong>")))
                    .andExpect(content().string(Matchers.containsString("<strong>15 august 2026</strong>")))
                    .andExpect(content().string(Matchers.containsString(
                            "Nu se pot face programări în acest interval. Vom procesa celelalte cereri de "
                                    + "programare imediat ce ne întoarcem. Vă mulțumim pentru înțelegere.")));
            mockMvc.perform(get("/hu"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("Irodánk")))
                    .andExpect(content().string(Matchers.containsString("<strong>01 augusztus 2026</strong>")))
                    .andExpect(content().string(Matchers.containsString("<strong>15 augusztus 2026</strong>")))
                    .andExpect(content().string(Matchers.containsString(
                            "között zárva van. Ebben az időszakban nem lehet időpontot foglalni. "
                                    + "A többi időpontfoglalási kérelmet visszatérésünk után azonnal feldolgozzuk. "
                                    + "Köszönjük a megértést.")));
        } finally {
            mockMvc.perform(post("/admin/settings/notification").with(csrf()));
        }
    }

    // Single-day vacation period (start == end): uses the dedicated "closed on <date>" template
    // instead of "closed between <date> and <date>", with only one bolded date and no "between"
    // connector rendered at all.
    @Test
    @WithMockUser(username = "titi", roles = "TECHNICIAN")
    void singleDayVacationShowsClosedOnTemplateInEachLocale() throws Exception {
        mockMvc.perform(post("/admin/settings/notification").with(csrf())
                        .param("enabled", "true")
                        .param("vacationStart", "2026-08-15")
                        .param("vacationEnd", "2026-08-15"))
                .andExpect(status().is3xxRedirection());
        try {
            mockMvc.perform(get("/en"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("Our office is closed on")))
                    .andExpect(content().string(Matchers.containsString("<strong>15 August 2026</strong>")))
                    .andExpect(content().string(Matchers.not(Matchers.containsString("between"))));
            mockMvc.perform(get("/ro"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("Biroul nostru este închis în data de")))
                    .andExpect(content().string(Matchers.containsString("<strong>15 august 2026</strong>")))
                    .andExpect(content().string(Matchers.containsString("Nu se pot face programări în această zi.")));
            mockMvc.perform(get("/hu"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("Irodánk zárva van a következő napon:")))
                    .andExpect(content().string(Matchers.containsString("<strong>15 augusztus 2026</strong>")));
        } finally {
            mockMvc.perform(post("/admin/settings/notification").with(csrf()));
        }
    }

    // Disabling clears the persisted vacation range end-to-end, not just the in-memory cache
    // asserted at the unit level (see SystemSettingsTest) - re-enabling afterwards without
    // supplying a new message or vacation range is rejected, and no banner text leaks through.
    @Test
    @WithMockUser(username = "titi", roles = "TECHNICIAN")
    void disablingClearsVacationRangeSoReEnablingWithoutNewInputIsRejected() throws Exception {
        mockMvc.perform(post("/admin/settings/notification").with(csrf())
                        .param("enabled", "true")
                        .param("vacationStart", "2026-08-01")
                        .param("vacationEnd", "2026-08-15"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/settings/notification").with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/settings/notification").with(csrf())
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        mockMvc.perform(get("/ro"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("<strong>"))));
    }

    // The booking page's date picker must grey out the announced vacation range so visitors
    // can't request an appointment during a closure the banner is actively telling them about.
    @Test
    @WithMockUser(username = "titi", roles = "TECHNICIAN")
    void bookingPageDisablesVacationRangeInDatePickerWhenVacationBannerActive() throws Exception {
        mockMvc.perform(post("/admin/settings/notification").with(csrf())
                        .param("enabled", "true")
                        .param("vacationStart", "2026-08-01")
                        .param("vacationEnd", "2026-08-15"))
                .andExpect(status().is3xxRedirection());
        try {
            mockMvc.perform(get("/en/book"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("var vacationStart = \"2026-08-01\";")))
                    .andExpect(content().string(Matchers.containsString("var vacationEnd = \"2026-08-15\";")));
        } finally {
            mockMvc.perform(post("/admin/settings/notification").with(csrf()));
        }
    }

    @Test
    @WithMockUser(username = "titi", roles = "TECHNICIAN")
    void bookingPageDoesNotDisableAnyDatesByDefault() throws Exception {
        mockMvc.perform(get("/en/book"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("var vacationStart = null;")))
                .andExpect(content().string(Matchers.containsString("var vacationEnd = null;")));
    }

    // A custom message showing instead of the vacation template means the *displayed* banner no
    // longer mentions the vacation dates at all, so the booking page shouldn't grey them out.
    @Test
    @WithMockUser(username = "titi", roles = "TECHNICIAN")
    void bookingPageDoesNotDisableDatesWhenCustomMessageOverridesVacationBanner() throws Exception {
        mockMvc.perform(post("/admin/settings/notification").with(csrf())
                        .param("enabled", "true")
                        .param("message", MESSAGE)
                        .param("vacationStart", "2026-08-01")
                        .param("vacationEnd", "2026-08-15"))
                .andExpect(status().is3xxRedirection());
        try {
            mockMvc.perform(get("/en/book"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("var vacationStart = null;")))
                    .andExpect(content().string(Matchers.containsString("var vacationEnd = null;")));
        } finally {
            mockMvc.perform(post("/admin/settings/notification").with(csrf()));
        }
    }

    // A custom typed message takes priority over an auto-generated vacation template even when
    // both are set.
    @Test
    @WithMockUser(username = "titi", roles = "TECHNICIAN")
    void customMessageTakesPriorityOverVacationRange() throws Exception {
        mockMvc.perform(post("/admin/settings/notification").with(csrf())
                        .param("enabled", "true")
                        .param("message", MESSAGE)
                        .param("vacationStart", "2026-08-01")
                        .param("vacationEnd", "2026-08-15"))
                .andExpect(status().is3xxRedirection());
        try {
            mockMvc.perform(get("/ro"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString(MESSAGE)))
                    .andExpect(content().string(Matchers.not(Matchers.containsString("01 august 2026"))));
        } finally {
            mockMvc.perform(post("/admin/settings/notification").with(csrf()));
        }
    }
}
