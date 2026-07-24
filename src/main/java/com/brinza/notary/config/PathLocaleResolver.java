package com.brinza.notary.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Resolves the request locale from the first URL path segment (/en/..., /ro/..., /hu/...)
 * rather than a cookie, session, or Accept-Language header, per the public site's
 * locale-aware routing scheme.
 */
public class PathLocaleResolver implements LocaleResolver {

    public static final Locale DEFAULT_LOCALE = Locale.of("ro");

    public static final List<Locale> SUPPORTED_LOCALES = List.of(
            Locale.of("en"), Locale.of("ro"), Locale.of("hu"));

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String[] segments = path.split("/", 3);
        if (segments.length > 1) {
            String candidate = segments[1];
            for (Locale locale : SUPPORTED_LOCALES) {
                if (locale.getLanguage().equals(candidate)) {
                    return locale;
                }
            }
        }
        return DEFAULT_LOCALE;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException(
                "Locale is determined by the URL path prefix (/en, /ro, /hu) and cannot be changed programmatically; "
                        + "link to the equivalent path under the target locale instead.");
    }
}
