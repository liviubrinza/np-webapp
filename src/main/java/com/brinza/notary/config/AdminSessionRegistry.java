package com.brinza.notary.config;

import com.brinza.notary.config.filters.AdminSessionCorrelationFilter;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which admin/technician username is behind each logged-in session, keyed by a
 * dedicated, randomly-generated "activity correlation ID" - deliberately NOT the servlet
 * session ID itself, since that's the live authentication token (session cookie value) and
 * must never be written to a log file a technician can read, or it would let a technician
 * hijack another admin's session by copying it out of the Logs/Activity panel.
 *
 * <p>The activity correlation ID is stored as an attribute on the {@link HttpSession} so it
 * stays stable for that session's lifetime, minted once (on login, via {@link #establish})
 * and reused by {@link AdminSessionCorrelationFilter} on every subsequent request in that
 * session. Entries are removed when the session ends (see {@link AdminSessionListener}).
 */
@Component
public class AdminSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(AdminSessionRegistry.class);

    static final String SESSION_ATTRIBUTE = "activityCorrelationId";

    private final Map<String, String> usernameByCorrelationId = new ConcurrentHashMap<>();

    /**
     * Returns this session's activity correlation ID, minting and registering a new one on
     * first use, and (re)registering the given username against it otherwise (idempotent, so
     * it's safe to call on every request of an authenticated session).
     */
    public String establish(HttpSession session, String username) {
        String correlationId = (String) session.getAttribute(SESSION_ATTRIBUTE);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            session.setAttribute(SESSION_ATTRIBUTE, correlationId);
            log.debug("Attached new session activity correlation ID for username={}", username);
        }
        usernameByCorrelationId.put(correlationId, username);
        return correlationId;
    }

    public void unregister(String correlationId) {
        String removed = usernameByCorrelationId.remove(correlationId);
        log.debug("Removed session tracking for username={}", removed);
    }

    public Optional<String> usernameFor(String correlationId) {
        return Optional.ofNullable(usernameByCorrelationId.get(correlationId));
    }
}
