package com.brinza.notary.config.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a fresh correlation ID to every incoming request and puts it in the SLF4J MDC,
 * so every log line emitted while handling the request - across security, controllers,
 * and services - can be tied back to it. Ordered ahead of the Spring Security filter
 * chain (registered at {@code SecurityProperties.DEFAULT_FILTER_ORDER = -100}) so
 * authentication-related log entries are correlated too. Always server-generated,
 * never taken from a client-supplied header, so it can't be spoofed or used to inject
 * arbitrary content into the logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        MDC.put(MDC_KEY, UUID.randomUUID().toString());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
