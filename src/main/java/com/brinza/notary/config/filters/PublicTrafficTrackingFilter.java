package com.brinza.notary.config.filters;

import com.brinza.notary.config.PathLocaleResolver;
import com.brinza.notary.service.PublicPage;
import com.brinza.notary.service.TrafficStatsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

/**
 * Feeds {@link TrafficStatsService} from real traffic: every GET request for one of the known
 * public pages (any locale) records a page view for the requesting IP. Only GETs for an actual
 * page count as a "view" - the booking form's POST submission isn't a page the visitor spent time
 * reading, and anything outside the locale-prefixed public routes (static assets, /admin/**,
 * /sitemap.xml, /error, ...) is left untouched.
 */
@Component
public class PublicTrafficTrackingFilter extends OncePerRequestFilter {

    private final TrafficStatsService trafficStatsService;

    public PublicTrafficTrackingFilter(TrafficStatsService trafficStatsService) {
        this.trafficStatsService = trafficStatsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (HttpMethod.GET.matches(request.getMethod())) {
            resolvePublicPage(request).ifPresent(page -> trafficStatsService.recordPageView(request.getRemoteAddr(), page));
        }
        filterChain.doFilter(request, response);
    }

    private static Optional<PublicPage> resolvePublicPage(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        for (Locale locale : PathLocaleResolver.SUPPORTED_LOCALES) {
            String prefix = "/" + locale.getLanguage();
            if (path.equals(prefix) || path.equals(prefix + "/")) {
                return PublicPage.fromPathAfterLocale("");
            }
            if (path.startsWith(prefix + "/")) {
                return PublicPage.fromPathAfterLocale(path.substring(prefix.length()));
            }
        }
        return Optional.empty();
    }
}
