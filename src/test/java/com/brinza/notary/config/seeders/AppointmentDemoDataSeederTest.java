package com.brinza.notary.config.seeders;

import com.brinza.notary.config.properties.AppointmentSeedProperties;
import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.domain.Service;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentDemoDataSeederTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ServiceRepository serviceRepository;

    private static AppointmentSeedProperties.AppointmentDefinition definitionFor(String serviceCode) {
        return new AppointmentSeedProperties.AppointmentDefinition("Client", "client@example.com", "0700000000",
                serviceCode, LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 7, 1, 9, 0),
                30, AppointmentStatus.CONFIRMED, "notes");
    }

    @Test
    void doesNothingWhenDisabled() {
        var properties = new AppointmentSeedProperties(List.of(definitionFor("code")));
        var seeder = new AppointmentDemoDataSeeder(false, properties, appointmentRepository, serviceRepository);

        seeder.run();

        verify(appointmentRepository, never()).saveAll(anyList());
        verify(serviceRepository, never()).findByCode(anyString());
    }

    @Test
    void doesNothingWhenAppointmentsTableIsNotEmpty() {
        var properties = new AppointmentSeedProperties(List.of(definitionFor("code")));
        when(appointmentRepository.count()).thenReturn(1L);
        var seeder = new AppointmentDemoDataSeeder(true, properties, appointmentRepository, serviceRepository);

        seeder.run();

        verify(appointmentRepository, never()).saveAll(anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    void seedsAppointmentsWithYamlSpecifiedStatusAndCreatedAt() {
        var properties = new AppointmentSeedProperties(List.of(definitionFor("code")));
        when(appointmentRepository.count()).thenReturn(0L);
        Service service = new Service(30, true);
        service.setCode("code");
        when(serviceRepository.findByCode("code")).thenReturn(Optional.of(service));
        var seeder = new AppointmentDemoDataSeeder(true, properties, appointmentRepository, serviceRepository);

        seeder.run();

        ArgumentCaptor<List<Appointment>> captor = ArgumentCaptor.forClass(List.class);
        verify(appointmentRepository).saveAll(captor.capture());
        List<Appointment> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(saved.get(0).getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 9, 0));
        assertThat(saved.get(0).getEndedAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 9, 30));
    }

    @Test
    void throwsWhenServiceCodeUnknown() {
        var properties = new AppointmentSeedProperties(List.of(definitionFor("missing-code")));
        when(appointmentRepository.count()).thenReturn(0L);
        when(serviceRepository.findByCode("missing-code")).thenReturn(Optional.empty());
        var seeder = new AppointmentDemoDataSeeder(true, properties, appointmentRepository, serviceRepository);

        assertThatThrownBy(seeder::run).isInstanceOf(IllegalStateException.class);
    }
}
