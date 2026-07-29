package com.brinza.notary.config.filters;

import com.brinza.notary.config.AdminSessionRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSessionCorrelationFilterTest {

    @Mock
    private AdminSessionRegistry adminSessionRegistry;

    private AdminSessionCorrelationFilter filter() {
        return new AdminSessionCorrelationFilter(adminSessionRegistry);
    }

    @AfterEach
    void clearState() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void establishesCorrelationIdForAuthenticatedNonAnonymousRequestWithSession() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("titi", null, List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(adminSessionRegistry.establish(session, "titi")).thenReturn("corr-1");

        filter().doFilterInternal(request, response, chain);

        verify(adminSessionRegistry).establish(session, "titi");
        verify(chain).doFilter(request, response);
    }

    @Test
    void skipsForAnonymousAuthentication() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter().doFilterInternal(request, response, chain);

        verify(adminSessionRegistry, never()).establish(any(), any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void skipsWhenNoAuthenticationPresent() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter().doFilterInternal(request, response, chain);

        verify(adminSessionRegistry, never()).establish(any(), any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void skipsWhenAuthenticatedButNoSessionExists() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("titi", null, List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getSession(false)).thenReturn(null);

        filter().doFilterInternal(request, response, chain);

        verify(adminSessionRegistry, never()).establish(any(), any());
        verify(chain).doFilter(request, response);
    }
}
