package com.brinza.notary.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PathLocaleResolverTest {

    private final PathLocaleResolver resolver = new PathLocaleResolver();

    @Test
    void resolvesEnglishFromPathPrefix() {
        assertThat(resolver.resolveLocale(requestFor("/en/services"))).isEqualTo(Locale.of("en"));
    }

    @Test
    void resolvesRomanianFromPathPrefix() {
        assertThat(resolver.resolveLocale(requestFor("/ro/services"))).isEqualTo(Locale.of("ro"));
    }

    @Test
    void resolvesHungarianFromPathPrefix() {
        assertThat(resolver.resolveLocale(requestFor("/hu/services"))).isEqualTo(Locale.of("hu"));
    }

    @Test
    void unsupportedPrefixFallsBackToDefault() {
        assertThat(resolver.resolveLocale(requestFor("/xx/services"))).isEqualTo(PathLocaleResolver.DEFAULT_LOCALE);
    }

    @Test
    void rootPathFallsBackToDefault() {
        assertThat(resolver.resolveLocale(requestFor("/"))).isEqualTo(PathLocaleResolver.DEFAULT_LOCALE);
    }

    @Test
    void usesOriginalErrorRequestUriWhenPresent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/error");
        when(request.getAttribute("jakarta.servlet.error.request_uri")).thenReturn("/en/some-page");

        assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.of("en"));
    }

    @Test
    void stripsNonEmptyContextPath() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/ctx");
        when(request.getRequestURI()).thenReturn("/ctx/hu/services");
        when(request.getAttribute("jakarta.servlet.error.request_uri")).thenReturn(null);

        assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.of("hu"));
    }

    @Test
    void setLocaleIsUnsupported() {
        assertThatThrownBy(() -> resolver.setLocale(requestFor("/en"), null, Locale.of("en")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static HttpServletRequest requestFor(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getAttribute("jakarta.servlet.error.request_uri")).thenReturn(null);
        return request;
    }
}
