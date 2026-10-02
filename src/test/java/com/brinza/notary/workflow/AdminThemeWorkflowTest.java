package com.brinza.notary.workflow;

import com.brinza.notary.domain.AdminTheme;
import com.brinza.notary.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Each admin user's theme is stored on their own account (default light) and applied to every
 * admin page they open via {@code data-bs-theme} on {@code <html>}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminThemeWorkflowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AdminUserRepository adminUserRepository;

    @Test
    void usersDefaultToLightTheme() throws Exception {
        assertThat(adminUserRepository.findByUsername("admin").orElseThrow().getTheme()).isEqualTo(AdminTheme.LIGHT);

        mockMvc.perform(get("/admin/appointments").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-bs-theme=\"light\"")));
    }

    @Test
    void themeChoiceIsPersistedPerUserAndAppliedToAdminPages() throws Exception {
        mockMvc.perform(post("/admin/profile/theme").with(user("titi").roles("TECHNICIAN")).with(csrf())
                        .param("theme", "DARK"))
                .andExpect(status().is3xxRedirection());

        assertThat(adminUserRepository.findByUsername("titi").orElseThrow().getTheme()).isEqualTo(AdminTheme.DARK);
        assertThat(adminUserRepository.findByUsername("admin").orElseThrow().getTheme()).isEqualTo(AdminTheme.LIGHT);

        mockMvc.perform(get("/admin/appointments").with(user("titi").roles("TECHNICIAN")))
                .andExpect(content().string(containsString("data-bs-theme=\"dark\"")));
        mockMvc.perform(get("/admin/calendar").with(user("titi").roles("TECHNICIAN")))
                .andExpect(content().string(containsString("data-bs-theme=\"dark\"")))
                .andExpect(content().string(containsString("flatpickr/dist/themes/dark.css")));
        mockMvc.perform(get("/admin/appointments").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(containsString("data-bs-theme=\"light\"")));
    }

    @Test
    void loginAndPublicPagesCarryNoTheme() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(content().string(not(containsString("data-bs-theme"))));
        mockMvc.perform(get("/ro/book"))
                .andExpect(content().string(not(containsString("data-bs-theme"))))
                .andExpect(content().string(not(containsString("themes/dark.css"))));
    }
}
