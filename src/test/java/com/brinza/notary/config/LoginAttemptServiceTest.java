package com.brinza.notary.config;

import com.brinza.notary.domain.AdminRole;
import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private SystemSettings systemSettings;

    private LoginAttemptService service() {
        return new LoginAttemptService(adminUserRepository, systemSettings);
    }

    @Test
    void notLockedBeforeAnyFailures() {
        LoginAttemptService service = service();

        assertThat(service.isLocked("titi")).isFalse();
    }

    @Test
    void doesNotLockBelowThreshold() {
        when(systemSettings.getLoginLockoutMaxAttempts()).thenReturn(3);
        LoginAttemptService service = service();

        service.recordFailure("titi");
        service.recordFailure("titi");

        assertThat(service.isLocked("titi")).isFalse();
    }

    @Test
    void locksAdminUserAfterReachingThreshold() {
        when(systemSettings.getLoginLockoutMaxAttempts()).thenReturn(3);
        when(systemSettings.getLoginLockoutLockDurationMinutes()).thenReturn(15);
        AdminUser adminUser = new AdminUser("titi", "hash", "Titi Full Name", AdminRole.TECHNICIAN);
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(adminUser));
        LoginAttemptService service = service();

        service.recordFailure("titi");
        service.recordFailure("titi");
        service.recordFailure("titi");

        assertThat(adminUser.isLocked()).isTrue();
        assertThat(adminUser.getLockUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    void isLockedReflectsPersistedAdminUserState() {
        AdminUser adminUser = new AdminUser("titi", "hash", "Titi Full Name", AdminRole.TECHNICIAN);
        adminUser.setLocked(true);
        adminUser.setLockUntil(LocalDateTime.now().plusMinutes(15));
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(adminUser));
        LoginAttemptService service = service();

        assertThat(service.isLocked("titi")).isTrue();
    }

    @Test
    void isLockedSurvivesAFreshServiceInstance() {
        // Simulates a restart: a brand new LoginAttemptService (no in-memory state at all)
        // still reports the account as locked, because the lock lives on the AdminUser row.
        AdminUser adminUser = new AdminUser("titi", "hash", "Titi Full Name", AdminRole.TECHNICIAN);
        adminUser.setLocked(true);
        adminUser.setLockUntil(LocalDateTime.now().plusMinutes(15));
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(adminUser));

        LoginAttemptService freshInstance = new LoginAttemptService(adminUserRepository, systemSettings);

        assertThat(freshInstance.isLocked("titi")).isTrue();
    }

    @Test
    void isLockedClearsAndReturnsFalseOnceLockUntilHasPassed() {
        AdminUser adminUser = new AdminUser("titi", "hash", "Titi Full Name", AdminRole.TECHNICIAN);
        adminUser.setLocked(true);
        adminUser.setLockUntil(LocalDateTime.now().minusMinutes(1));
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(adminUser));
        LoginAttemptService service = service();

        boolean locked = service.isLocked("titi");

        assertThat(locked).isFalse();
        assertThat(adminUser.isLocked()).isFalse();
        assertThat(adminUser.getLockUntil()).isNull();
    }

    @Test
    void doesNotLockAnUnrelatedUsername() {
        when(systemSettings.getLoginLockoutMaxAttempts()).thenReturn(1);
        when(systemSettings.getLoginLockoutLockDurationMinutes()).thenReturn(15);
        AdminUser adminUser = new AdminUser("titi", "hash", "Titi Full Name", AdminRole.TECHNICIAN);
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(adminUser));
        LoginAttemptService service = service();

        service.recordFailure("titi");

        assertThat(service.isLocked("other-user")).isFalse();
    }

    @Test
    void successResetsInMemoryFailureCount() {
        // Threshold 2: without the reset, this failure/success/failure sequence would still
        // total 2 failures and trip the lock. Asserting the repository (the lock-persisting
        // step) was never even consulted proves the reset actually happened.
        when(systemSettings.getLoginLockoutMaxAttempts()).thenReturn(2);
        LoginAttemptService service = service();

        service.recordFailure("titi");
        service.recordSuccess("titi");
        service.recordFailure("titi");

        verify(adminUserRepository, never()).findByUsername(any());
    }

    @Test
    void authenticationFailureEventLocksAfterThreshold() {
        when(systemSettings.getLoginLockoutMaxAttempts()).thenReturn(1);
        when(systemSettings.getLoginLockoutLockDurationMinutes()).thenReturn(15);
        AdminUser adminUser = new AdminUser("titi", "hash", "Titi Full Name", AdminRole.TECHNICIAN);
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(adminUser));
        LoginAttemptService service = service();
        var authentication = new UsernamePasswordAuthenticationToken("titi", "wrong");
        var event = new AuthenticationFailureBadCredentialsEvent(authentication, new BadCredentialsException("bad"));

        service.onAuthenticationFailure(event);

        assertThat(adminUser.isLocked()).isTrue();
    }

    @Test
    void authenticationSuccessEventResetsFailureCount() {
        when(systemSettings.getLoginLockoutMaxAttempts()).thenReturn(2);
        LoginAttemptService service = service();
        service.recordFailure("titi");
        var event = new AuthenticationSuccessEvent(new UsernamePasswordAuthenticationToken("titi", null));

        service.onAuthenticationSuccess(event);
        service.recordFailure("titi");

        verify(adminUserRepository, never()).findByUsername(any());
    }
}
