package com.brinza.notary.config;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Removes a session's entry from {@link AdminSessionRegistry} once the session ends, whether
 * via explicit logout (which invalidates the session) or timeout, so the registry doesn't grow
 * unbounded with stale entries for sessions that no longer exist.
 */
@Component
public class AdminSessionListener implements HttpSessionListener {

    private static final Logger log = LoggerFactory.getLogger(AdminSessionListener.class);

    private final AdminSessionRegistry adminSessionRegistry;

    public AdminSessionListener(AdminSessionRegistry adminSessionRegistry) {
        this.adminSessionRegistry = adminSessionRegistry;
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        log.info("sessionDestroyed called");
        Object correlationId = se.getSession().getAttribute(AdminSessionRegistry.SESSION_ATTRIBUTE);
        if (correlationId instanceof String id) {
            adminSessionRegistry.unregister(id);
        }
    }
}
