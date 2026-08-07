package com.brinza.notary.config;

import com.brinza.notary.domain.SystemSetting;
import com.brinza.notary.repository.SystemSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.logging.LogLevel;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemSettingsTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    private SystemSettings settings(boolean mailEnabledDefault) {
        return new SystemSettings(systemSettingRepository, mailEnabledDefault, 5, 15, LogLevel.INFO);
    }

    @Test
    void loadFallsBackToDefaultWhenNoRowExists() {
        when(systemSettingRepository.findBySettingKey("mail.enabled")).thenReturn(Optional.empty());
        SystemSettings settings = settings(false);

        settings.load();

        assertThat(settings.isMailEnabled()).isFalse();
    }

    @Test
    void loadFallsBackToDefaultTrueWhenNoRowExists() {
        when(systemSettingRepository.findBySettingKey("mail.enabled")).thenReturn(Optional.empty());
        SystemSettings settings = settings(true);

        settings.load();

        assertThat(settings.isMailEnabled()).isTrue();
    }

    @Test
    void loadUsesDbValueOverDefault() {
        SystemSetting stored = new SystemSetting("mail.enabled");
        stored.setSettingValue("true");
        when(systemSettingRepository.findBySettingKey("mail.enabled")).thenReturn(Optional.of(stored));
        SystemSettings settings = settings(false);

        settings.load();

        assertThat(settings.isMailEnabled()).isTrue();
    }

    @Test
    void setMailEnabledCreatesRowWhenNoneExistsAndUpdatesCache() {
        when(systemSettingRepository.findBySettingKey("mail.enabled")).thenReturn(Optional.empty());
        SystemSettings settings = settings(false);

        settings.setMailEnabled(true);

        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(systemSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getSettingValue()).isEqualTo("true");
        assertThat(settings.isMailEnabled()).isTrue();
    }

    @Test
    void setMailEnabledUpdatesExistingRow() {
        SystemSetting existing = new SystemSetting("mail.enabled");
        existing.setSettingValue("false");
        when(systemSettingRepository.findBySettingKey("mail.enabled")).thenReturn(Optional.of(existing));
        SystemSettings settings = settings(false);

        settings.setMailEnabled(true);

        verify(systemSettingRepository).save(existing);
        assertThat(existing.getSettingValue()).isEqualTo("true");
    }

    @Test
    void loadFallsBackToLoginLockoutDefaultsWhenNoRowsExist() {
        when(systemSettingRepository.findBySettingKey("mail.enabled")).thenReturn(Optional.empty());
        when(systemSettingRepository.findBySettingKey("login-lockout.max-attempts")).thenReturn(Optional.empty());
        when(systemSettingRepository.findBySettingKey("login-lockout.lock-duration-minutes")).thenReturn(Optional.empty());
        SystemSettings settings = settings(false);

        settings.load();

        assertThat(settings.getLoginLockoutMaxAttempts()).isEqualTo(5);
        assertThat(settings.getLoginLockoutLockDurationMinutes()).isEqualTo(15);
    }

    @Test
    void loadUsesDbValueForLoginLockoutSettingsOverDefault() {
        SystemSetting maxAttempts = new SystemSetting("login-lockout.max-attempts");
        maxAttempts.setSettingValue("7");
        SystemSetting lockDuration = new SystemSetting("login-lockout.lock-duration-minutes");
        lockDuration.setSettingValue("30");
        when(systemSettingRepository.findBySettingKey("mail.enabled")).thenReturn(Optional.empty());
        when(systemSettingRepository.findBySettingKey("login-lockout.max-attempts")).thenReturn(Optional.of(maxAttempts));
        when(systemSettingRepository.findBySettingKey("login-lockout.lock-duration-minutes")).thenReturn(Optional.of(lockDuration));
        SystemSettings settings = settings(false);

        settings.load();

        assertThat(settings.getLoginLockoutMaxAttempts()).isEqualTo(7);
        assertThat(settings.getLoginLockoutLockDurationMinutes()).isEqualTo(30);
    }

    @Test
    void setLoginLockoutMaxAttemptsUpdatesCache() {
        when(systemSettingRepository.findBySettingKey("login-lockout.max-attempts")).thenReturn(Optional.empty());
        SystemSettings settings = settings(false);

        settings.setLoginLockoutMaxAttempts(10);

        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(systemSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getSettingValue()).isEqualTo("10");
        assertThat(settings.getLoginLockoutMaxAttempts()).isEqualTo(10);
    }

    @Test
    void setLoginLockoutMaxAttemptsRejectsValuesBelowOne() {
        SystemSettings settings = settings(false);

        assertThatThrownBy(() -> settings.setLoginLockoutMaxAttempts(0)).isInstanceOf(IllegalArgumentException.class);
        verify(systemSettingRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void setLoginLockoutLockDurationMinutesUpdatesCache() {
        when(systemSettingRepository.findBySettingKey("login-lockout.lock-duration-minutes")).thenReturn(Optional.empty());
        SystemSettings settings = settings(false);

        settings.setLoginLockoutLockDurationMinutes(45);

        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(systemSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getSettingValue()).isEqualTo("45");
        assertThat(settings.getLoginLockoutLockDurationMinutes()).isEqualTo(45);
    }

    @Test
    void setLoginLockoutLockDurationMinutesRejectsValuesBelowOne() {
        SystemSettings settings = settings(false);

        assertThatThrownBy(() -> settings.setLoginLockoutLockDurationMinutes(0)).isInstanceOf(IllegalArgumentException.class);
        verify(systemSettingRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loadFallsBackToLogLevelDefaultWhenNoRowExists() {
        when(systemSettingRepository.findBySettingKey("mail.enabled")).thenReturn(Optional.empty());
        when(systemSettingRepository.findBySettingKey("login-lockout.max-attempts")).thenReturn(Optional.empty());
        when(systemSettingRepository.findBySettingKey("login-lockout.lock-duration-minutes")).thenReturn(Optional.empty());
        when(systemSettingRepository.findBySettingKey("log.level")).thenReturn(Optional.empty());
        SystemSettings settings = settings(false);

        settings.load();

        assertThat(settings.getLogLevel()).isEqualTo(LogLevel.INFO);
    }

    @Test
    void loadUsesDbValueForLogLevelOverDefault() {
        when(systemSettingRepository.findBySettingKey("mail.enabled")).thenReturn(Optional.empty());
        when(systemSettingRepository.findBySettingKey("login-lockout.max-attempts")).thenReturn(Optional.empty());
        when(systemSettingRepository.findBySettingKey("login-lockout.lock-duration-minutes")).thenReturn(Optional.empty());
        SystemSetting stored = new SystemSetting("log.level");
        stored.setSettingValue("DEBUG");
        when(systemSettingRepository.findBySettingKey("log.level")).thenReturn(Optional.of(stored));
        SystemSettings settings = settings(false);

        settings.load();

        assertThat(settings.getLogLevel()).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void setLogLevelCreatesRowWhenNoneExistsAndUpdatesCache() {
        when(systemSettingRepository.findBySettingKey("log.level")).thenReturn(Optional.empty());
        SystemSettings settings = settings(false);

        settings.setLogLevel(LogLevel.WARN);

        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(systemSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getSettingValue()).isEqualTo("WARN");
        assertThat(settings.getLogLevel()).isEqualTo(LogLevel.WARN);
    }

    @Test
    void setLogLevelUpdatesExistingRow() {
        SystemSetting existing = new SystemSetting("log.level");
        existing.setSettingValue("INFO");
        when(systemSettingRepository.findBySettingKey("log.level")).thenReturn(Optional.of(existing));
        SystemSettings settings = settings(false);

        settings.setLogLevel(LogLevel.TRACE);

        verify(systemSettingRepository).save(existing);
        assertThat(existing.getSettingValue()).isEqualTo("TRACE");
    }
}
