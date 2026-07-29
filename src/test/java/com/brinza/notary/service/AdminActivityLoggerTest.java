package com.brinza.notary.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class AdminActivityLoggerTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger activityLogger;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        activityLogger = (Logger) LoggerFactory.getLogger("AdminActivity");
        originalLevel = activityLogger.getLevel();
        // Other tests in the suite boot a Spring context that applies this session's
        // logging.level.root=WARN to the shared, JVM-wide Logback context, which would
        // otherwise silently swallow this logger's INFO-level activity entries depending on
        // run order - pin it explicitly so this test doesn't depend on execution order.
        activityLogger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        activityLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        activityLogger.detachAppender(appender);
        activityLogger.setLevel(originalLevel);
        SecurityContextHolder.clearContext();
    }

    @Test
    void logsWithExplicitUsername() {
        new AdminActivityLogger().log("titi", "Did something");

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).isEqualTo("[titi] Did something");
    }

    @Test
    void logsWithUsernameFromSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null));

        new AdminActivityLogger().log("Logged in");

        assertThat(appender.list.get(0).getFormattedMessage()).isEqualTo("[admin] Logged in");
    }

    @Test
    void logsUnknownWhenNoAuthentication() {
        new AdminActivityLogger().log("Anonymous action");

        assertThat(appender.list.get(0).getFormattedMessage()).isEqualTo("[unknown] Anonymous action");
    }
}
