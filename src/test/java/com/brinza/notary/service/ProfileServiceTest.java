package com.brinza.notary.service;

import com.brinza.notary.domain.AdminRole;
import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private final ProfileService service() {
        return new ProfileService(adminUserRepository, passwordEncoder);
    }

    @Test
    void throwsWhenUserNotFound() {
        when(adminUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().changePassword("ghost", "current", "newpw"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void rejectsWrongCurrentPassword() {
        AdminUser user = new AdminUser("titi", "hash", "Titi Full Name", AdminRole.TECHNICIAN);
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service().changePassword("titi", "wrong", "newpw"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatesPasswordHashOnSuccess() {
        AdminUser user = new AdminUser("titi", "oldHash", "Titi Full Name", AdminRole.TECHNICIAN);
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("newpw")).thenReturn("newHash");

        service().changePassword("titi", "current", "newpw");

        assertThat(user.getPasswordHash()).isEqualTo("newHash");
    }

    @Test
    void getFullNameReturnsStoredFullName() {
        AdminUser user = new AdminUser("titi", "hash", "Titi Full Name", AdminRole.TECHNICIAN);
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(user));

        assertThat(service().getFullName("titi")).isEqualTo("Titi Full Name");
    }

    @Test
    void getFullNameThrowsWhenUserNotFound() {
        when(adminUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getFullName("ghost"))
                .isInstanceOf(NoSuchElementException.class);
    }
}
