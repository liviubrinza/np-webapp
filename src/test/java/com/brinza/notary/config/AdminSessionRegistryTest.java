package com.brinza.notary.config;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminSessionRegistryTest {

    private final AdminSessionRegistry registry = new AdminSessionRegistry();

    @Test
    void establishMintsNewCorrelationIdOnFirstCall() {
        HttpSession session = fakeSession();

        String correlationId = registry.establish(session, "titi");

        assertThat(correlationId).isNotBlank();
        assertThat(registry.usernameFor(correlationId)).contains("titi");
    }

    @Test
    void establishIsIdempotentAndReRegistersUsername() {
        HttpSession session = fakeSession();

        String first = registry.establish(session, "titi");
        String second = registry.establish(session, "admin");

        assertThat(second).isEqualTo(first);
        assertThat(registry.usernameFor(first)).contains("admin");
    }

    @Test
    void unregisterRemovesMapping() {
        HttpSession session = fakeSession();
        String correlationId = registry.establish(session, "titi");

        registry.unregister(correlationId);

        assertThat(registry.usernameFor(correlationId)).isEmpty();
    }

    @Test
    void usernameForUnknownCorrelationIdIsEmpty() {
        assertThat(registry.usernameFor("unknown")).isEmpty();
    }

    private static HttpSession fakeSession() {
        Map<String, Object> attributes = new HashMap<>();
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(AdminSessionRegistry.SESSION_ATTRIBUTE))
                .thenAnswer(inv -> attributes.get(AdminSessionRegistry.SESSION_ATTRIBUTE));
        doAnswer(inv -> {
            attributes.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(session).setAttribute(anyString(), any());
        return session;
    }
}
