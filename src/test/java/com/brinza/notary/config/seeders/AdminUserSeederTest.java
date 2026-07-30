package com.brinza.notary.config.seeders;

import com.brinza.notary.config.properties.AdminUserSeedProperties;
import com.brinza.notary.domain.AdminRole;
import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserSeederTest {

    @Mock
    private AdminUserRepository adminUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsMissingAccountsOnlyAndLeavesExistingUntouched() {
        var existingDef = new AdminUserSeedProperties.AdminUserDefinition("titi", "titi", "Titi Full Name", AdminRole.TECHNICIAN);
        var newDef = new AdminUserSeedProperties.AdminUserDefinition("newuser", "pw", "New User", AdminRole.ADMIN);
        var properties = new AdminUserSeedProperties(List.of(existingDef, newDef));
        when(adminUserRepository.findByUsername("titi"))
                .thenReturn(Optional.of(new AdminUser("titi", "existingHash", "Titi Full Name", AdminRole.TECHNICIAN)));
        when(adminUserRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("encodedPw");

        new AdminUserSeeder(properties, adminUserRepository, passwordEncoder).run();

        verify(adminUserRepository, never()).save(argThat(u -> u.getUsername().equals("titi")));
        verify(adminUserRepository).save(argThat(u ->
                u.getUsername().equals("newuser") && u.getPasswordHash().equals("encodedPw") && u.getRole() == AdminRole.ADMIN));
    }
}
