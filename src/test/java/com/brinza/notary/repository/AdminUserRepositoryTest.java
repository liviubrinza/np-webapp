package com.brinza.notary.repository;

import com.brinza.notary.domain.AdminRole;
import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.migration.V11__AddAppointmentEndedAt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest's restricted component scan does not pick up our one Java-based Flyway
// migration (a plain @Component); without this @Import, Flyway silently skips it and the
// appointments.ended_at column never gets created.
@DataJpaTest
@Import(V11__AddAppointmentEndedAt.class)
class AdminUserRepositoryTest {

    @Autowired
    private AdminUserRepository repository;

    @Test
    void findByUsernameReturnsMatchingUser() {
        repository.save(new AdminUser("titi", "hash", AdminRole.TECHNICIAN));

        assertThat(repository.findByUsername("titi")).isPresent();
        assertThat(repository.findByUsername("ghost")).isEmpty();
    }

    @Test
    void findAllByOrderByUsernameAscSortsAlphabetically() {
        repository.save(new AdminUser("zed", "hash", AdminRole.ADMIN));
        repository.save(new AdminUser("alpha", "hash", AdminRole.ADMIN));

        List<AdminUser> all = repository.findAllByOrderByUsernameAsc();

        assertThat(all).extracting(AdminUser::getUsername).containsExactly("alpha", "zed");
    }
}
