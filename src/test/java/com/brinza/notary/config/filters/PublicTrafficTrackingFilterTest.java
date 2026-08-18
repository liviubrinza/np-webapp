package com.brinza.notary.config.filters;

import com.brinza.notary.service.PublicPage;
import com.brinza.notary.service.TrafficStatsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PublicTrafficTrackingFilterTest {

    private final TrafficStatsService trafficStatsService = mock(TrafficStatsService.class);
    private final PublicTrafficTrackingFilter filter = new PublicTrafficTrackingFilter(trafficStatsService);

    @ParameterizedTest
    @CsvSource({
            "/ro,HOME",
            "/en,HOME",
            "/hu,HOME",
            "/ro/,HOME",
            "/ro/services,SERVICES",
            "/en/contact,CONTACT",
            "/hu/book,BOOK",
            "/ro/book/confirmation,BOOK_CONFIRMATION"
    })
    void recordsAGetOnAKnownPublicPage(String path, PublicPage expectedPage) throws Exception {
        HttpServletRequest request = requestFor("GET", path);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");

        filter.doFilterInternal(request, response, chain);

        verify(trafficStatsService).recordPageView(eq("203.0.113.5"), eq(expectedPage));
        verify(chain).doFilter(request, response);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/statistics", "/sitemap.xml", "/robots.txt", "/css/site.css", "/error", "/fr/services"})
    void doesNotRecordNonPublicOrUnsupportedLocalePaths(String path) throws Exception {
        HttpServletRequest request = requestFor("GET", path);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verifyNoInteractions(trafficStatsService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doesNotRecordPostRequestsEvenToAKnownPublicPage() throws Exception {
        HttpServletRequest request = requestFor("POST", "/ro/book");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(trafficStatsService, never()).recordPageView(any(), any());
        verify(chain).doFilter(request, response);
    }

    private static HttpServletRequest requestFor(String method, String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn("");
        return request;
    }
}
