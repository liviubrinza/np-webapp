package com.brinza.notary.config;

import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Locks an admin username out after too many failed login attempts in a row, to slow down
 * password-guessing against the admin login page. The threshold and lock duration are
 * runtime-editable (see {@link SystemSettings}), and once a lock actually trips it's persisted
 * onto the {@link AdminUser} row ({@code locked}/{@code lockUntil}), so it survives an app
 * restart.
 *
 * <p>The in-progress failure count leading up to that threshold, however, is only kept in
 * memory - a restart before the threshold is reached resets it. Only a lock that has already
 * tripped is guaranteed to survive.
 *
 * <p>Wires itself directly to Spring Security's authentication events, so no other class needs
 * to know this exists except {@link AdminUserDetailsService}, which asks {@link #isLocked}.
 */
@Component
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final AdminUserRepository adminUserRepository;
    private final SystemSettings systemSettings;

    private final Map<String, AtomicInteger> failureCountByUsername = new ConcurrentHashMap<>();

    public LoginAttemptService(AdminUserRepository adminUserRepository, SystemSettings systemSettings) {
        this.adminUserRepository = adminUserRepository;
        this.systemSettings = systemSettings;
    }

    // @Transactional here (not just on recordFailure) matters: this is the method Spring's
    // event multicaster actually calls from outside this bean, so it's the only place the
    // @Transactional proxy is guaranteed to apply. Calling recordFailure(...) from within this
    // same class is a plain self-invocation that bypasses the proxy entirely - without an
    // active transaction already open here, recordFailure's own @Transactional would be a
    // no-op and the entity mutation would silently never reach the database.
    @Transactional
    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        recordFailure(event.getAuthentication().getName());
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        recordSuccess(event.getAuthentication().getName());
    }

    @Transactional
    public void recordFailure(String username) {
        int failureCount = failureCountByUsername
                .computeIfAbsent(username, key -> new AtomicInteger())
                .incrementAndGet();

        if (failureCount < systemSettings.getLoginLockoutMaxAttempts()) {
            return;
        }
        adminUserRepository.findByUsername(username).ifPresent(adminUser -> {
            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(systemSettings.getLoginLockoutLockDurationMinutes());
            adminUser.setLocked(true);
            adminUser.setLockUntil(lockUntil);
            log.debug("Locking username={} until={} after {} failed attempt(s)", username, lockUntil, failureCount);
        });
        failureCountByUsername.remove(username);
    }

    public void recordSuccess(String username) {
        failureCountByUsername.remove(username);
    }

    @Transactional
    public boolean isLocked(String username) {
        return adminUserRepository.findByUsername(username)
                .map(adminUser -> {
                    if (!adminUser.isLocked()) {
                        return false;
                    }
                    LocalDateTime lockUntil = adminUser.getLockUntil();
                    if (lockUntil == null || !lockUntil.isAfter(LocalDateTime.now())) {
                        log.debug("Lock for username={} has expired, clearing", username);
                        adminUser.setLocked(false);
                        adminUser.setLockUntil(null);
                        return false;
                    }
                    return true;
                })
                .orElse(false);
    }
}
