package com.brinza.notary.controller.admin;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.domain.AdminRole;
import com.brinza.notary.dto.AdminUserView;
import com.brinza.notary.service.AdminActivityLogger;
import com.brinza.notary.service.AdminUserManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminUserAdminController.class)
@Import(AdminSessionRegistry.class)
@WithMockUser(roles = "TECHNICIAN")
class AdminUserAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AdminUserManagementService adminUserManagementService;
    @MockitoBean
    private AdminActivityLogger adminActivityLogger;

    @Test
    void listRendersAdminsFromService() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        when(adminUserManagementService.listAdmins()).thenReturn(
                List.of(new AdminUserView(1L, "titi", "Titi Full Name", AdminRole.TECHNICIAN, createdAt, null, false, null)));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/list"))
                .andExpect(model().attribute("admins",
                        List.of(new AdminUserView(1L, "titi", "Titi Full Name", AdminRole.TECHNICIAN, createdAt, null, false, null))));
    }

    @Test
    void detailRendersAdminFromService() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        when(adminUserManagementService.getAdmin(1L)).thenReturn(
                new AdminUserView(1L, "titi", "Titi Full Name", AdminRole.TECHNICIAN, createdAt, null, false, null));

        mockMvc.perform(get("/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/detail"))
                .andExpect(content().string(containsString("Titi Full Name")));
    }

    @Test
    void detailRendersLockedStateAndLockUntil() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime lockUntil = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(adminUserManagementService.getAdmin(1L)).thenReturn(
                new AdminUserView(1L, "titi", "Titi Full Name", AdminRole.TECHNICIAN, createdAt, null, true, lockUntil));

        mockMvc.perform(get("/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Blocat")))
                .andExpect(content().string(containsString("Deblochează")));
    }

    @Test
    void unlockSuccessRedirectsToDetailWithFlashMessage() throws Exception {
        mockMvc.perform(post("/admin/users/1/unlock").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/1"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void newFormDefaultsRoleToAdmin() throws Exception {
        mockMvc.perform(get("/admin/users/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"))
                .andExpect(model().attribute("adminUserForm",
                        org.hamcrest.Matchers.hasProperty("role", org.hamcrest.Matchers.is(AdminRole.ADMIN))));
    }

    @Test
    void createWithBlankUsernameRedisplaysFormWithoutCallingService() throws Exception {
        mockMvc.perform(post("/admin/users").with(csrf())
                        .param("username", "")
                        .param("fullName", "New User")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"));
    }

    @Test
    void createSuccessRedirectsWithFlashMessage() throws Exception {
        mockMvc.perform(post("/admin/users").with(csrf())
                        .param("username", "newuser")
                        .param("password", "secretpw")
                        .param("fullName", "New User")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void createDuplicateUsernameRedisplaysFormWithError() throws Exception {
        doThrow(new IllegalArgumentException("Numele de utilizator este deja folosit."))
                .when(adminUserManagementService).create(any());

        mockMvc.perform(post("/admin/users").with(csrf())
                        .param("username", "titi")
                        .param("password", "secretpw")
                        .param("fullName", "New User")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void createBlankFullNameRedisplaysFormWithoutCallingService() throws Exception {
        mockMvc.perform(post("/admin/users").with(csrf())
                        .param("username", "newuser")
                        .param("password", "secretpw")
                        .param("fullName", "")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"));
    }

    @Test
    void deleteSuccessRedirectsWithFlashMessage() throws Exception {
        mockMvc.perform(post("/admin/users/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void deleteSelfRedirectsWithFlashError() throws Exception {
        doThrow(new IllegalArgumentException("Nu va puteti sterge propriul cont."))
                .when(adminUserManagementService).delete(any(), any());

        mockMvc.perform(post("/admin/users/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("error"));
    }
}
