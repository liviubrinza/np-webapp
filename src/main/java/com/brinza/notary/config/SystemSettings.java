package com.brinza.notary.config;

import com.brinza.notary.domain.SystemSetting;
import com.brinza.notary.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
    private static final String KEY_LOG_LEVEL = "log.level";
    private static final String KEY_NOTIFICATION_ENABLED = "notification.enabled";
    private static final String KEY_NOTIFICATION_MESSAGE = "notification.message";
    private static final String KEY_NOTIFICATION_VACATION_START = "notification.vacation-start";
    private static final String KEY_NOTIFICATION_VACATION_END = "notification.vacation-end";

    /** Base package of the app's own code - the logger scope the runtime-editable log level applies to. */
    private static final String APP_LOGGER_NAME = "com.brinza.notary";

    private final SystemSettingRepository systemSettingRepository;
    private final LoggingSystem loggingSystem;
    private final boolean mailEnabledDefault;
    private final int loginLockoutMaxAttemptsDefault;
    private final int loginLockoutLockDurationMinutesDefault;
    private final LogLevel logLevelDefault;

    private volatile boolean mailEnabled;
    private volatile int loginLockoutMaxAttempts;
    private volatile int loginLockoutLockDurationMinutes;
    private volatile LogLevel logLevel;
    private volatile boolean notificationEnabled;
    private volatile String notificationMessage;
    private volatile LocalDate notificationVacationStart;
    private volatile LocalDate notificationVacationEnd;

    public SystemSettings(SystemSettingRepository systemSettingRepository,
                           @Value("${app.mail.enabled:false}") boolean mailEnabledDefault,
                           @Value("${app.security.login-lockout.max-attempts:5}") int loginLockoutMaxAttemptsDefault,
                           @Value("${app.security.login-lockout.lock-duration-minutes:15}") int loginLockoutLockDurationMinutesDefault,
                           @Value("${logging.level.com.brinza.notary:INFO}") LogLevel logLevelDefault) {
        this.systemSettingRepository = systemSettingRepository;
        this.loggingSystem = LoggingSystem.get(getClass().getClassLoader());
        this.mailEnabledDefault = mailEnabledDefault;
        this.loginLockoutMaxAttemptsDefault = loginLockoutMaxAttemptsDefault;
        this.loginLockoutLockDurationMinutesDefault = loginLockoutLockDurationMinutesDefault;
        this.logLevelDefault = logLevelDefault;
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
        logLevel = systemSettingRepository.findBySettingKey(KEY_LOG_LEVEL)
                .map(setting -> LogLevel.valueOf(setting.getSettingValue()))
                .orElse(logLevelDefault);
        loggingSystem.setLogLevel(APP_LOGGER_NAME, logLevel);
        notificationEnabled = systemSettingRepository.findBySettingKey(KEY_NOTIFICATION_ENABLED)
                .map(setting -> Boolean.parseBoolean(setting.getSettingValue()))
                .orElse(false);
        notificationMessage = systemSettingRepository.findBySettingKey(KEY_NOTIFICATION_MESSAGE)
                .map(SystemSetting::getSettingValue)
                .orElse("");
        notificationVacationStart = systemSettingRepository.findBySettingKey(KEY_NOTIFICATION_VACATION_START)
                .map(SystemSetting::getSettingValue)
                .filter(value -> !value.isBlank())
                .map(LocalDate::parse)
                .orElse(null);
        notificationVacationEnd = systemSettingRepository.findBySettingKey(KEY_NOTIFICATION_VACATION_END)
                .map(SystemSetting::getSettingValue)
                .filter(value -> !value.isBlank())
                .map(LocalDate::parse)
                .orElse(null);
        log.info("System settings loaded: mailEnabled={}, loginLockoutMaxAttempts={}, loginLockoutLockDurationMinutes={}, logLevel={}, notificationEnabled={}",
                mailEnabled, loginLockoutMaxAttempts, loginLockoutLockDurationMinutes, logLevel, notificationEnabled);
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

    public LogLevel getLogLevel() {
        return logLevel;
    }

    @Transactional
    public void setLogLevel(LogLevel value) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(KEY_LOG_LEVEL)
                .orElseGet(() -> new SystemSetting(KEY_LOG_LEVEL));
        setting.setSettingValue(value.name());
        systemSettingRepository.save(setting);
        logLevel = value;
        loggingSystem.setLogLevel(APP_LOGGER_NAME, value);
        log.info("System setting updated: logLevel={}", value);
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

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public LocalDate getNotificationVacationStart() {
        return notificationVacationStart;
    }

    public LocalDate getNotificationVacationEnd() {
        return notificationVacationEnd;
    }

    @Transactional
    public void setNotification(boolean enabled, String message, LocalDate vacationStart, LocalDate vacationEnd) {
        String normalizedMessage = message == null ? "" : message.trim();
        LocalDate normalizedVacationStart = vacationStart;
        LocalDate normalizedVacationEnd = vacationEnd;
        if (enabled) {
            if ((normalizedVacationStart == null) != (normalizedVacationEnd == null)) {
                throw new IllegalArgumentException(
                        "Trebuie completate ambele date ale perioadei de vacanță, sau niciuna.");
            }
            if (normalizedVacationStart != null && normalizedVacationStart.isAfter(normalizedVacationEnd)) {
                throw new IllegalArgumentException(
                        "Data de început a vacanței trebuie să fie înainte de data de sfârșit.");
            }
            boolean hasVacationRange = normalizedVacationStart != null;
            if (normalizedMessage.isEmpty() && !hasVacationRange) {
                throw new IllegalArgumentException(
                        "Mesajul de notificare nu poate fi gol când banner-ul este activat, decât dacă este selectată o perioadă de vacanță.");
            }
        } else {
            // Disabling always clears the stored message and vacation period too, so stale content
            // from a previous notification can never resurface just by flipping the toggle back on.
            normalizedMessage = "";
            normalizedVacationStart = null;
            normalizedVacationEnd = null;
        }
        SystemSetting enabledSetting = systemSettingRepository.findBySettingKey(KEY_NOTIFICATION_ENABLED)
                .orElseGet(() -> new SystemSetting(KEY_NOTIFICATION_ENABLED));
        enabledSetting.setSettingValue(Boolean.toString(enabled));
        systemSettingRepository.save(enabledSetting);
        SystemSetting messageSetting = systemSettingRepository.findBySettingKey(KEY_NOTIFICATION_MESSAGE)
                .orElseGet(() -> new SystemSetting(KEY_NOTIFICATION_MESSAGE));
        messageSetting.setSettingValue(normalizedMessage);
        systemSettingRepository.save(messageSetting);
        SystemSetting vacationStartSetting = systemSettingRepository.findBySettingKey(KEY_NOTIFICATION_VACATION_START)
                .orElseGet(() -> new SystemSetting(KEY_NOTIFICATION_VACATION_START));
        vacationStartSetting.setSettingValue(normalizedVacationStart == null ? "" : normalizedVacationStart.toString());
        systemSettingRepository.save(vacationStartSetting);
        SystemSetting vacationEndSetting = systemSettingRepository.findBySettingKey(KEY_NOTIFICATION_VACATION_END)
                .orElseGet(() -> new SystemSetting(KEY_NOTIFICATION_VACATION_END));
        vacationEndSetting.setSettingValue(normalizedVacationEnd == null ? "" : normalizedVacationEnd.toString());
        systemSettingRepository.save(vacationEndSetting);
        notificationEnabled = enabled;
        notificationMessage = normalizedMessage;
        notificationVacationStart = normalizedVacationStart;
        notificationVacationEnd = normalizedVacationEnd;
        log.info("System setting updated: notificationEnabled={}, notificationVacationStart={}, notificationVacationEnd={}",
                enabled, normalizedVacationStart, normalizedVacationEnd);
    }
}
