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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void createPersistsAccountWithEncodedPassword() throws Exception {
        mockMvc.perform(post("/admin/users").with(csrf())
                        .param("username", "workflow-new-user")
                        .param("password", "secretpw")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        AdminUser created = adminUserRepository.findByUsername("workflow-new-user").orElseThrow();
        assertThat(created.getPasswordHash()).isNotEqualTo("secretpw");
        assertThat(passwordEncoder.matches("secretpw", created.getPasswordHash())).isTrue();
        assertThat(created.getRole()).isEqualTo(AdminRole.ADMIN);
    }

    @Test
    void createDuplicateUsernameRedisplaysFormAndDoesNotDuplicate() throws Exception {
        mockMvc.perform(post("/admin/users").with(csrf())
                        .param("username", "titi")
                        .param("password", "secretpw")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"));

        assertThat(adminUserRepository.findByUsername("titi")).isPresent();
    }

    @Test
    void editUpdatesRole() throws Exception {
        AdminUser existing = adminUserRepository.save(
                new AdminUser("workflow-editable", passwordEncoder.encode("pw"), AdminRole.ADMIN));

        mockMvc.perform(post("/admin/users/" + existing.getId()).with(csrf())
                        .param("username", "workflow-editable")
                        .param("role", "TECHNICIAN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        AdminUser reloaded = adminUserRepository.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(AdminRole.TECHNICIAN);
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
                new AdminUser("workflow-deletable", passwordEncoder.encode("pw"), AdminRole.ADMIN));

        mockMvc.perform(post("/admin/users/" + other.getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        assertThat(adminUserRepository.findById(other.getId())).isEmpty();
    }
}
