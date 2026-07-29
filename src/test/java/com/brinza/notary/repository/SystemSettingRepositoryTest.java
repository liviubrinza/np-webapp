package com.brinza.notary.repository;

import com.brinza.notary.domain.SystemSetting;
import com.brinza.notary.migration.V11__AddAppointmentEndedAt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// @DataJpaTest's restricted component scan does not pick up our one Java-based Flyway
// migration (a plain @Component); without this @Import, Flyway silently skips it and the
// appointments.ended_at column never gets created.
@DataJpaTest
@Import(V11__AddAppointmentEndedAt.class)
class SystemSettingRepositoryTest {

    @Autowired
    private SystemSettingRepository repository;

    @Test
    void findBySettingKeyReturnsMatch() {
        SystemSetting setting = new SystemSetting("mail.enabled");
        setting.setSettingValue("true");
        repository.saveAndFlush(setting);

        assertThat(repository.findBySettingKey("mail.enabled")).isPresent();
        assertThat(repository.findBySettingKey("unknown.key")).isEmpty();
    }

    @Test
    void duplicateSettingKeyViolatesUniqueConstraint() {
        SystemSetting first = new SystemSetting("mail.enabled");
        first.setSettingValue("true");
        repository.saveAndFlush(first);

        SystemSetting duplicate = new SystemSetting("mail.enabled");
        duplicate.setSettingValue("false");

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
