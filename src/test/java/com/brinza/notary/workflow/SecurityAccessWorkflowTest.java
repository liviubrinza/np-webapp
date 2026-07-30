package com.brinza.notary.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityAccessWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousIsRedirectedToLoginForAdminRoutes() throws Exception {
        mockMvc.perform(get("/admin/appointments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
    }

    // admin/login is served by a plain WebConfig#addViewController, not a @Controller, so no
    // @WebMvcTest slice covers it - this is the only page in the app without render coverage
    // elsewhere. Also confirms th:action (not a raw action=) actually gets the CSRF token
    // injected, which is what the CI raw-action= guard assumes is true for every POST form.
    @Test
    void anonymousCanRenderLoginPage() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"))
                .andExpect(content().string(containsString("Autentificare Admin")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    // The rendered "Acces interzis" error page (see error.html) was verified manually against a
    // real running server - MockMvc's simulated dispatcher doesn't perform the container-level
    // forward to /error that a real servlet container does on sendError(403), so only the status
    // code - which is what actually protects the SecurityConfig role rule - is checked here.
    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoleGetsForbiddenOnTechnicianOnlyRoute() throws Exception {
        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void technicianRoleCanAccessTechnicianOnlyRoute() throws Exception {
        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk());
    }

    @Test
    void realLoginWithCorrectCredentialsSucceeds() throws Exception {
        mockMvc.perform(post("/admin/login").with(csrf())
                        .param("username", "titi")
                        .param("password", "titi"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void realLoginWithWrongPasswordFails() throws Exception {
        mockMvc.perform(post("/admin/login").with(csrf())
                        .param("username", "titi")
                        .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"));
    }
}
