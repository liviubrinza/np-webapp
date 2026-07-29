package com.brinza.notary.config;

import com.brinza.notary.domain.SystemSetting;
import com.brinza.notary.repository.SystemSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemSettingsTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Test
    void loadFallsBackToDefaultWhenNoRowExists() {
        when(systemSettingRepository.findBySettingKey("mail.enabled")).thenReturn(Optional.empty());
        SystemSettings settings = new SystemSettings(systemSettingRepository, false);

        settings.load();

        assertThat(settings.isMailEnabled()).isFalse();
    }

    @Test
    void loadFallsBackToDefaultTrueWhenNoRowExists() {
        when(systemSettingRepository.findBySettingKey("mail.enabled")).thenReturn(Optional.empty());
        SystemSettings settings = new SystemSettings(systemSettingRepository, true);

        settings.load();

        assertThat(settings.isMailEnabled()).isTrue();
    }

    @Test
    void loadUsesDbValueOverDefault() {
        SystemSetting stored = new SystemSetting("mail.enabled");
        stored.setSettingValue("true");
        when(systemSettingRepository.findBySettingKey("mail.enabled")).thenReturn(Optional.of(stored));
        SystemSettings settings = new SystemSettings(systemSettingRepository, false);

        settings.load();

        assertThat(settings.isMailEnabled()).isTrue();
    }

    @Test
    void setMailEnabledCreatesRowWhenNoneExistsAndUpdatesCache() {
        when(systemSettingRepository.findBySettingKey("mail.enabled")).thenReturn(Optional.empty());
        SystemSettings settings = new SystemSettings(systemSettingRepository, false);

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
        SystemSettings settings = new SystemSettings(systemSettingRepository, false);

        settings.setMailEnabled(true);

        verify(systemSettingRepository).save(existing);
        assertThat(existing.getSettingValue()).isEqualTo("true");
    }
}
