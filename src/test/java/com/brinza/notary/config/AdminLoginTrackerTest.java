package com.brinza.notary.config;

import com.brinza.notary.config.filters.CorrelationIdFilter;
import com.brinza.notary.domain.AdminRole;
import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.repository.AdminUserRepository;
import com.brinza.notary.service.AdminActivityLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLoginTrackerTest {

    @Mock
    private AdminUserRepository adminUserRepository;
    @Mock
    private AdminSessionRegistry adminSessionRegistry;
    @Mock
    private AdminActivityLogger adminActivityLogger;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;

    private AdminLoginTracker tracker() {
        return new AdminLoginTracker(adminUserRepository, adminSessionRegistry, adminActivityLogger, request);
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void nonUserDetailsPrincipalIsANoOp() {
        var event = new AuthenticationSuccessEvent(new UsernamePasswordAuthenticationToken("justAString", null));

        tracker().onAuthenticationSuccess(event);

        verifyNoInteractions(adminSessionRegistry, adminActivityLogger);
        verify(adminUserRepository, never()).findByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void updatesLastLoginSetsMdcAndLogsActivityWhenUserFound() {
        when(request.getSession(true)).thenReturn(session);
        when(adminSessionRegistry.establish(session, "titi")).thenReturn("corr-abc");
        AdminUser adminUser = new AdminUser("titi", "hash", AdminRole.TECHNICIAN);
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(adminUser));

        var event = new AuthenticationSuccessEvent(userDetailsAuthentication("titi"));
        tracker().onAuthenticationSuccess(event);

        assertThat(adminUser.getLastLogin()).isNotNull();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("corr-abc");
        verify(adminActivityLogger).log("titi", "User 'titi' logged in");
    }

    @Test
    void doesNotThrowWhenUserNotFoundInRepository() {
        when(request.getSession(true)).thenReturn(session);
        when(adminSessionRegistry.establish(session, "ghost")).thenReturn("corr-xyz");
        when(adminUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        var event = new AuthenticationSuccessEvent(userDetailsAuthentication("ghost"));

        assertThatCode(() -> tracker().onAuthenticationSuccess(event)).doesNotThrowAnyException();
        verify(adminActivityLogger).log("ghost", "User 'ghost' logged in");
    }

    private static UsernamePasswordAuthenticationToken userDetailsAuthentication(String username) {
        UserDetails principal = User.withUsername(username).password("hash").authorities("ROLE_TECHNICIAN").build();
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
