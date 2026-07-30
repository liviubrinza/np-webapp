package com.brinza.notary.service;

import com.brinza.notary.domain.AdminRole;
import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.dto.AdminUserForm;
import com.brinza.notary.dto.AdminUserView;
import com.brinza.notary.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminUserManagementService service;

    private AdminUserManagementService service() {
        return new AdminUserManagementService(adminUserRepository, passwordEncoder);
    }

    @Test
    void createRejectsDuplicateUsername() {
        service = service();
        AdminUserForm form = formFor("titi", "secret", "Full Name", AdminRole.TECHNICIAN);
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(mock(AdminUser.class)));

        assertThatThrownBy(() -> service.create(form)).isInstanceOf(IllegalArgumentException.class);
        verify(adminUserRepository, never()).save(any());
    }

    @Test
    void createRejectsBlankPassword() {
        service = service();
        AdminUserForm form = formFor("newuser", " ", "Full Name", AdminRole.ADMIN);
        when(adminUserRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(form)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsShortPassword() {
        service = service();
        AdminUserForm form = formFor("newuser", "abc", "Full Name", AdminRole.ADMIN);
        when(adminUserRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(form)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createSavesEncodedPasswordAndFullName() {
        service = service();
        AdminUserForm form = formFor("newuser", "secretpw", "New User", AdminRole.ADMIN);
        when(adminUserRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secretpw")).thenReturn("ENCODED");

        service.create(form);

        org.mockito.ArgumentCaptor<AdminUser> captor = org.mockito.ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserRepository).save(captor.capture());
        assertThat(captor.getValue().getFullName()).isEqualTo("New User");
    }

    @Test
    void updateRejectsUsernameAlreadyUsedByAnotherAccount() {
        service = service();
        AdminUser existing = new AdminUser("old", "hash", "Old Name", AdminRole.ADMIN);
        AdminUser other = new AdminUser("taken", "hash2", "Other Name", AdminRole.ADMIN);
        setId(other, 2L);
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(adminUserRepository.findByUsername("taken")).thenReturn(Optional.of(other));

        AdminUserForm form = formFor("taken", null, "Some Name", AdminRole.ADMIN);

        assertThatThrownBy(() -> service.update(1L, form)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateWithoutPasswordKeepsExistingHash() {
        service = service();
        AdminUser existing = new AdminUser("titi", "originalHash", "Titi Full Name", AdminRole.TECHNICIAN);
        setId(existing, 1L);
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(existing));

        service.update(1L, formFor("titi", null, "Titi Full Name", AdminRole.TECHNICIAN));

        assertThat(existing.getPasswordHash()).isEqualTo("originalHash");
    }

    @Test
    void updateWithPasswordReEncodesHash() {
        service = service();
        AdminUser existing = new AdminUser("titi", "originalHash", "Titi Full Name", AdminRole.TECHNICIAN);
        setId(existing, 1L);
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newpassword")).thenReturn("newHash");

        service.update(1L, formFor("titi", "newpassword", "Titi Full Name", AdminRole.TECHNICIAN));

        assertThat(existing.getPasswordHash()).isEqualTo("newHash");
    }

    @Test
    void updateChangesFullName() {
        service = service();
        AdminUser existing = new AdminUser("titi", "originalHash", "Titi Full Name", AdminRole.TECHNICIAN);
        setId(existing, 1L);
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(existing));

        service.update(1L, formFor("titi", null, "Updated Full Name", AdminRole.TECHNICIAN));

        assertThat(existing.getFullName()).isEqualTo("Updated Full Name");
    }

    @Test
    void deleteRejectsDeletingSelf() {
        service = service();
        AdminUser self = new AdminUser("titi", "hash", "Titi Full Name", AdminRole.TECHNICIAN);
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> service.delete(1L, "titi")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteRemovesOtherAccount() {
        service = service();
        AdminUser other = new AdminUser("someone-else", "hash", "Someone Else", AdminRole.ADMIN);
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(other));

        service.delete(1L, "titi");

        verify(adminUserRepository, times(1)).delete(other);
    }

    @Test
    void getAdminThrowsWhenNotFound() {
        service = service();
        when(adminUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAdmin(99L)).isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void listAdminsMapsToViews() {
        service = service();
        AdminUser user = new AdminUser("titi", "hash", "Titi Full Name", AdminRole.TECHNICIAN);
        when(adminUserRepository.findAllByOrderByUsernameAsc()).thenReturn(List.of(user));

        List<AdminUserView> views = service.listAdmins();

        assertThat(views).hasSize(1);
        assertThat(views.get(0).username()).isEqualTo("titi");
        assertThat(views.get(0).fullName()).isEqualTo("Titi Full Name");
    }

    private static AdminUserForm formFor(String username, String password, String fullName, AdminRole role) {
        AdminUserForm form = new AdminUserForm();
        form.setUsername(username);
        form.setPassword(password);
        form.setFullName(fullName);
        form.setRole(role);
        return form;
    }

    private static void setId(AdminUser user, Long id) {
        try {
            var field = AdminUser.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
