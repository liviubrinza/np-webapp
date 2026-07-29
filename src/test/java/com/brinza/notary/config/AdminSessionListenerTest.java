package com.brinza.notary.config;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSessionListenerTest {

    @Mock
    private AdminSessionRegistry adminSessionRegistry;
    @Mock
    private HttpSessionEvent sessionEvent;
    @Mock
    private HttpSession session;

    private AdminSessionListener listener() {
        return new AdminSessionListener(adminSessionRegistry);
    }

    @Test
    void unregistersCorrelationIdWhenPresent() {
        when(sessionEvent.getSession()).thenReturn(session);
        when(session.getAttribute(AdminSessionRegistry.SESSION_ATTRIBUTE)).thenReturn("corr-123");

        listener().sessionDestroyed(sessionEvent);

        verify(adminSessionRegistry).unregister("corr-123");
    }

    @Test
    void doesNothingWhenNoCorrelationIdAttribute() {
        when(sessionEvent.getSession()).thenReturn(session);
        when(session.getAttribute(AdminSessionRegistry.SESSION_ATTRIBUTE)).thenReturn(null);

        listener().sessionDestroyed(sessionEvent);

        verify(adminSessionRegistry, never()).unregister(org.mockito.ArgumentMatchers.anyString());
    }
}
