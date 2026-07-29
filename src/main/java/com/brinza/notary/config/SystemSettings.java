package com.brinza.notary.config;

import com.brinza.notary.domain.SystemSetting;
import com.brinza.notary.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runtime-editable behavior flags, backed by the {@code system_settings} table and mutable at
 * runtime from the technician-only Configurare page. On startup each known setting is read from
 * the DB; one that's never been edited (no row yet) falls back to its {@code application.yml}
 * default rather than being treated as missing configuration - see {@code app.mail.enabled}.
 *
 * <p>Values are cached in memory after {@link #load()} so callers on the request path (e.g.
 * {@link com.brinza.notary.service.AppointmentEmailService}) never hit the DB just to check a
 * flag; {@link #setMailEnabled(boolean)} keeps that cache in sync with each write.
 */
@Component
public class SystemSettings {

    private static final Logger log = LoggerFactory.getLogger(SystemSettings.class);

    private static final String KEY_MAIL_ENABLED = "mail.enabled";

    private final SystemSettingRepository systemSettingRepository;
    private final boolean mailEnabledDefault;

    private volatile boolean mailEnabled;

    public SystemSettings(SystemSettingRepository systemSettingRepository,
                           @Value("${app.mail.enabled:false}") boolean mailEnabledDefault) {
        this.systemSettingRepository = systemSettingRepository;
        this.mailEnabledDefault = mailEnabledDefault;
    }

    @PostConstruct
    void load() {
        mailEnabled = systemSettingRepository.findBySettingKey(KEY_MAIL_ENABLED)
                .map(setting -> Boolean.parseBoolean(setting.getSettingValue()))
                .orElse(mailEnabledDefault);
        log.info("System settings loaded: mailEnabled={}", mailEnabled);
    }

    public boolean isMailEnabled() {
        return mailEnabled;
    }

    @Transactional
    public void setMailEnabled(boolean value) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(KEY_MAIL_ENABLED)
                .orElseGet(() -> new SystemSetting(KEY_MAIL_ENABLED));
        setting.setSettingValue(Boolean.toString(value));
        systemSettingRepository.save(setting);
        mailEnabled = value;
        log.info("System setting updated: mailEnabled={}", value);
    }
}
