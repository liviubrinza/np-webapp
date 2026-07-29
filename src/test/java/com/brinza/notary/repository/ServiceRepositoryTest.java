package com.brinza.notary.repository;

import com.brinza.notary.domain.Service;
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
class ServiceRepositoryTest {

    @Autowired
    private ServiceRepository repository;

    @Test
    void findByActiveTrueExcludesInactiveServices() {
        Service active = new Service(30, true);
        active.setCode("active-code");
        Service inactive = new Service(30, false);
        inactive.setCode("inactive-code");
        repository.save(active);
        repository.save(inactive);

        List<Service> result = repository.findByActiveTrue();

        assertThat(result).extracting(Service::getCode).containsExactly("active-code");
    }

    @Test
    void findByCodeReturnsMatch() {
        Service service = new Service(30, true);
        service.setCode("some-code");
        repository.save(service);

        assertThat(repository.findByCode("some-code")).isPresent();
        assertThat(repository.findByCode("unknown-code")).isEmpty();
    }
}
