package com.brinza.notary.config;

import com.brinza.notary.repository.AdminUserRepository;
import com.brinza.notary.service.AdminActivityLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class AdminLoginTracker {

    private static final Logger log = LoggerFactory.getLogger(AdminLoginTracker.class);

    private final AdminUserRepository adminUserRepository;
    private final AdminSessionRegistry adminSessionRegistry;
    private final AdminActivityLogger adminActivityLogger;
    private final HttpServletRequest request;

    public AdminLoginTracker(AdminUserRepository adminUserRepository, AdminSessionRegistry adminSessionRegistry,
                              AdminActivityLogger adminActivityLogger, HttpServletRequest request) {
        this.adminUserRepository = adminUserRepository;
        this.adminSessionRegistry = adminSessionRegistry;
        this.adminActivityLogger = adminActivityLogger;
        this.request = request;
    }

    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (!(event.getAuthentication().getPrincipal() instanceof UserDetails userDetails)) {
            log.debug("Authentication principal is not a UserDetails, skipping last-login update");
            return;
        }
        String username = userDetails.getUsername();

        // Establish (rather than wait for AdminSessionCorrelationFilter on the *next* request)
        // so this "logged in" activity entry already carries the same correlation ID as the
        // rest of the session, instead of a one-off per-request ID that the registry never sees.
        String correlationId = adminSessionRegistry.establish(request.getSession(true), username);
        MDC.put(CorrelationIdFilter.MDC_KEY, correlationId);
        adminActivityLogger.log(username, "User '%s' logged in".formatted(username));

        log.debug("Updating last-login timestamp for username={}", username);
        adminUserRepository.findByUsername(username)
                .ifPresentOrElse(
                        adminUser -> adminUser.setLastLogin(LocalDateTime.now()),
                        () -> log.debug("No AdminUser found for username={}, last-login not updated", username));
    }
}
