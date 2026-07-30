package com.brinza.notary.workflow;

import com.brinza.notary.domain.AdminRole;
import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "titi", roles = "TECHNICIAN")
class AdminUserCrudWorkflowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AdminUserRepository adminUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createPersistsAccountWithEncodedPasswordAndFullName() throws Exception {
        mockMvc.perform(post("/admin/users").with(csrf())
                        .param("username", "workflow-new-user")
                        .param("password", "secretpw")
                        .param("fullName", "Workflow New User")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        AdminUser created = adminUserRepository.findByUsername("workflow-new-user").orElseThrow();
        assertThat(created.getPasswordHash()).isNotEqualTo("secretpw");
        assertThat(passwordEncoder.matches("secretpw", created.getPasswordHash())).isTrue();
        assertThat(created.getRole()).isEqualTo(AdminRole.ADMIN);
        assertThat(created.getFullName()).isEqualTo("Workflow New User");
    }

    @Test
    void createDuplicateUsernameRedisplaysFormAndDoesNotDuplicate() throws Exception {
        mockMvc.perform(post("/admin/users").with(csrf())
                        .param("username", "titi")
                        .param("password", "secretpw")
                        .param("fullName", "Duplicate Attempt")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"));

        assertThat(adminUserRepository.findByUsername("titi")).isPresent();
    }

    @Test
    void editUpdatesRoleAndFullName() throws Exception {
        AdminUser existing = adminUserRepository.save(
                new AdminUser("workflow-editable", passwordEncoder.encode("pw"), "Original Full Name", AdminRole.ADMIN));

        mockMvc.perform(post("/admin/users/" + existing.getId()).with(csrf())
                        .param("username", "workflow-editable")
                        .param("fullName", "Updated Full Name")
                        .param("role", "TECHNICIAN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        AdminUser reloaded = adminUserRepository.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(AdminRole.TECHNICIAN);
        assertThat(reloaded.getFullName()).isEqualTo("Updated Full Name");
    }

    @Test
    void detailPageRendersFullName() throws Exception {
        AdminUser existing = adminUserRepository.save(
                new AdminUser("workflow-detail", passwordEncoder.encode("pw"), "Detail Full Name", AdminRole.ADMIN));

        mockMvc.perform(get("/admin/users/" + existing.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/detail"))
                .andExpect(content().string(containsString("Detail Full Name")));
    }

    @Test
    void unlockClearsLockBeforeLockUntilIsReachedAndAllowsImmediateLogin() throws Exception {
        AdminUser locked = adminUserRepository.save(new AdminUser("workflow-locked",
                passwordEncoder.encode("correct-password"), "Locked User", AdminRole.ADMIN));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/admin/login").with(csrf())
                    .param("username", "workflow-locked")
                    .param("password", "wrong-password"));
        }
        assertThat(adminUserRepository.findById(locked.getId()).orElseThrow().isLocked()).isTrue();

        mockMvc.perform(post("/admin/users/" + locked.getId() + "/unlock").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + locked.getId()))
                .andExpect(flash().attributeExists("success"));

        AdminUser reloaded = adminUserRepository.findById(locked.getId()).orElseThrow();
        assertThat(reloaded.isLocked()).isFalse();
        assertThat(reloaded.getLockUntil()).isNull();

        mockMvc.perform(post("/admin/login").with(csrf())
                        .param("username", "workflow-locked")
                        .param("password", "correct-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void deletingOwnAccountIsRejected() throws Exception {
        AdminUser self = adminUserRepository.findByUsername("titi").orElseThrow();

        mockMvc.perform(post("/admin/users/" + self.getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        assertThat(adminUserRepository.findByUsername("titi")).isPresent();
    }

    @Test
    void deletingOtherAccountRemovesIt() throws Exception {
        AdminUser other = adminUserRepository.save(
                new AdminUser("workflow-deletable", passwordEncoder.encode("pw"), "Deletable User", AdminRole.ADMIN));

        mockMvc.perform(post("/admin/users/" + other.getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        assertThat(adminUserRepository.findById(other.getId())).isEmpty();
    }
}
