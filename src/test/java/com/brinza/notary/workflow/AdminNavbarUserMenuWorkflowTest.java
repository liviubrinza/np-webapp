package com.brinza.notary.workflow;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the admin navbar's user menu: the icon + username button opens a dropdown holding
 * the Profil link and the (CSRF-protected) Deconectare form.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "titi", roles = "TECHNICIAN")
class AdminNavbarUserMenuWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void userMenuIsRenderedAsAnIconAndUsernameDropdownButton() throws Exception {
        mockMvc.perform(get("/admin/appointments"))
                .andExpect(status().isOk())
                // The URL carries a content hash - see StaticAssetVersioningTest.
                .andExpect(content().string(Matchers.matchesPattern(
                        "(?s).*/images/user_icon(-[0-9a-f]{32})?\\.png.*")))
                .andExpect(content().string(Matchers.containsString("data-bs-toggle=\"dropdown\"")))
                .andExpect(content().string(Matchers.containsString("titi")));
    }

    @Test
    void dropdownHoldsProfileLinkAndLogoutForm() throws Exception {
        String html = mockMvc.perform(get("/admin/appointments"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String menu = html.substring(html.indexOf("<ul class=\"dropdown-menu dropdown-menu-lg-end\">"));
        menu = menu.substring(0, menu.indexOf("</ul>"));

        org.assertj.core.api.Assertions.assertThat(menu)
                .contains("href=\"/admin/profile\"")
                .contains("Profil")
                .contains("action=\"/admin/logout\"")
                .contains("Deconectare")
                .contains("name=\"_csrf\"");
    }
}
