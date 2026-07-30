package com.brinza.notary.config;

import com.brinza.notary.domain.AdminRole;
import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.repository.AdminUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deliberately NOT @Transactional (unlike the other workflow tests): a class-level test
 * transaction would keep a single Hibernate session open for the whole test, which would mask
 * the exact bug this guards against - {@link LoginAttemptService#onAuthenticationFailure} calling
 * {@code this.recordFailure(...)} is a same-class call that bypasses the @Transactional proxy,
 * so without @Transactional directly on onAuthenticationFailure itself, the entity mutation
 * would silently never reach the database in production even though every other test (which all
 * run inside an ambient test transaction) would still pass.
 */
@SpringBootTest
class LoginAttemptServicePersistenceTest {

    @Autowired
    private LoginAttemptService loginAttemptService;
    @Autowired
    private AdminUserRepository adminUserRepository;

    private Long createdId;

    @AfterEach
    void cleanUp() {
        if (createdId != null) {
            adminUserRepository.deleteById(createdId);
        }
    }

    @Test
    void lockTrippedByRealAuthenticationFailureEventsIsActuallyPersisted() {
        AdminUser saved = adminUserRepository.save(new AdminUser("persistence-check-user", "hash",
                "Persistence Check", AdminRole.ADMIN));
        createdId = saved.getId();

        for (int i = 0; i < 5; i++) {
            var authentication = new UsernamePasswordAuthenticationToken("persistence-check-user", "wrong");
            var event = new AuthenticationFailureBadCredentialsEvent(authentication, new BadCredentialsException("bad"));
            loginAttemptService.onAuthenticationFailure(event);
        }

        AdminUser reloaded = adminUserRepository.findById(createdId).orElseThrow();
        assertThat(reloaded.isLocked()).isTrue();
        assertThat(reloaded.getLockUntil()).isNotNull();
    }
}
