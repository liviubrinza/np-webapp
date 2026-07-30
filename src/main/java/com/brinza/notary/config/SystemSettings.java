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
 * flag; the setters keep that cache in sync with each write.
 */
@Component
public class SystemSettings {

    private static final Logger log = LoggerFactory.getLogger(SystemSettings.class);

    private static final String KEY_MAIL_ENABLED = "mail.enabled";
    private static final String KEY_LOGIN_LOCKOUT_MAX_ATTEMPTS = "login-lockout.max-attempts";
    private static final String KEY_LOGIN_LOCKOUT_LOCK_DURATION_MINUTES = "login-lockout.lock-duration-minutes";

    private final SystemSettingRepository systemSettingRepository;
    private final boolean mailEnabledDefault;
    private final int loginLockoutMaxAttemptsDefault;
    private final int loginLockoutLockDurationMinutesDefault;

    private volatile boolean mailEnabled;
    private volatile int loginLockoutMaxAttempts;
    private volatile int loginLockoutLockDurationMinutes;

    public SystemSettings(SystemSettingRepository systemSettingRepository,
                           @Value("${app.mail.enabled:false}") boolean mailEnabledDefault,
                           @Value("${app.security.login-lockout.max-attempts:5}") int loginLockoutMaxAttemptsDefault,
                           @Value("${app.security.login-lockout.lock-duration-minutes:15}") int loginLockoutLockDurationMinutesDefault) {
        this.systemSettingRepository = systemSettingRepository;
        this.mailEnabledDefault = mailEnabledDefault;
        this.loginLockoutMaxAttemptsDefault = loginLockoutMaxAttemptsDefault;
        this.loginLockoutLockDurationMinutesDefault = loginLockoutLockDurationMinutesDefault;
    }

    @PostConstruct
    void load() {
        mailEnabled = systemSettingRepository.findBySettingKey(KEY_MAIL_ENABLED)
                .map(setting -> Boolean.parseBoolean(setting.getSettingValue()))
                .orElse(mailEnabledDefault);
        loginLockoutMaxAttempts = systemSettingRepository.findBySettingKey(KEY_LOGIN_LOCKOUT_MAX_ATTEMPTS)
                .map(setting -> Integer.parseInt(setting.getSettingValue()))
                .orElse(loginLockoutMaxAttemptsDefault);
        loginLockoutLockDurationMinutes = systemSettingRepository.findBySettingKey(KEY_LOGIN_LOCKOUT_LOCK_DURATION_MINUTES)
                .map(setting -> Integer.parseInt(setting.getSettingValue()))
                .orElse(loginLockoutLockDurationMinutesDefault);
        log.info("System settings loaded: mailEnabled={}, loginLockoutMaxAttempts={}, loginLockoutLockDurationMinutes={}",
                mailEnabled, loginLockoutMaxAttempts, loginLockoutLockDurationMinutes);
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

    public int getLoginLockoutMaxAttempts() {
        return loginLockoutMaxAttempts;
    }

    public int getLoginLockoutLockDurationMinutes() {
        return loginLockoutLockDurationMinutes;
    }

    @Transactional
    public void setLoginLockoutMaxAttempts(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Numărul de încercări trebuie să fie cel puțin 1.");
        }
        SystemSetting setting = systemSettingRepository.findBySettingKey(KEY_LOGIN_LOCKOUT_MAX_ATTEMPTS)
                .orElseGet(() -> new SystemSetting(KEY_LOGIN_LOCKOUT_MAX_ATTEMPTS));
        setting.setSettingValue(Integer.toString(value));
        systemSettingRepository.save(setting);
        loginLockoutMaxAttempts = value;
        log.info("System setting updated: loginLockoutMaxAttempts={}", value);
    }

    @Transactional
    public void setLoginLockoutLockDurationMinutes(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Durata de blocare trebuie să fie de cel puțin 1 minut.");
        }
        SystemSetting setting = systemSettingRepository.findBySettingKey(KEY_LOGIN_LOCKOUT_LOCK_DURATION_MINUTES)
                .orElseGet(() -> new SystemSetting(KEY_LOGIN_LOCKOUT_LOCK_DURATION_MINUTES));
        setting.setSettingValue(Integer.toString(value));
        systemSettingRepository.save(setting);
        loginLockoutLockDurationMinutes = value;
        log.info("System setting updated: loginLockoutLockDurationMinutes={}", value);
    }
}
