package com.brinza.notary.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * For an authenticated (non-anonymous) admin/technician request, overrides the per-request
 * {@link CorrelationIdFilter#MDC_KEY} value with this session's stable activity correlation ID
 * (see {@link AdminSessionRegistry}), so every log line for the rest of this request - and
 * every other request in the same login session - carries the same ID. Ordered to run after
 * the entire Spring Security filter chain (registered as a single filter at order -100) so
 * {@link SecurityContextHolder} reliably reflects this request's authentication.
 */
@Component
@Order(-99)
public class AdminSessionCorrelationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminSessionCorrelationFilter.class);

    private final AdminSessionRegistry adminSessionRegistry;

    public AdminSessionCorrelationFilter(AdminSessionRegistry adminSessionRegistry) {
        this.adminSessionRegistry = adminSessionRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                log.debug("Attaching session activity correlation ID for username={}", authentication.getName());
                String correlationId = adminSessionRegistry.establish(session, authentication.getName());
                MDC.put(CorrelationIdFilter.MDC_KEY, correlationId);
            }
        }
        filterChain.doFilter(request, response);
    }
}
