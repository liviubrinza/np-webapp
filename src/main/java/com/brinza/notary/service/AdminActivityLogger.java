package com.brinza.notary.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Records a curated trail of meaningful admin/technician actions - button presses that submit
 * data or change state, not routine page navigation - for the "Admin activity" statistics
 * panel. Writes to a dedicated logger name ("AdminActivity") rather than the calling class's
 * own logger, so {@code LogViewerService} can pick these lines out of the shared daily log file
 * without needing to guess which classes/methods represent "real" activity.
 *
 * <p>The acting username is embedded directly in the persisted message as {@code "[username] ..."}
 * (not just left to be resolved later via {@link AdminSessionRegistry}), so historical activity
 * stays fully readable even after that session's in-memory registry entry is gone (e.g. after a
 * server restart or the user logging out).
 */
@Component
public class AdminActivityLogger {

    private static final Logger activityLog = LoggerFactory.getLogger("AdminActivity");

    public void log(String action) {
        log(currentUsername(), action);
    }

    /**
     * For callers where {@link SecurityContextHolder} doesn't yet reflect the acting user - e.g.
     * an {@code AuthenticationSuccessEvent} listener fires before the SecurityContext is updated
     * with the newly-authenticated principal - so the username must be supplied explicitly.
     */
    public void log(String username, String action) {
        activityLog.info("[{}] {}", username, action);
    }

    private static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "unknown";
    }
}
