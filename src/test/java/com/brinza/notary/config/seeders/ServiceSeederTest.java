package com.brinza.notary.config.seeders;

import com.brinza.notary.config.properties.ServiceSeedProperties;
import com.brinza.notary.domain.Service;
import com.brinza.notary.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceSeederTest {

    @Mock
    private ServiceRepository serviceRepository;

    private ServiceSeeder seeder(ServiceSeedProperties properties) {
        return new ServiceSeeder(properties, serviceRepository);
    }

    @Test
    void createsNewServiceWithTranslations() {
        var definition = new ServiceSeedProperties.ServiceDefinition("new-code", 30,
                Map.of("ro", new ServiceSeedProperties.Translation("Nume", "Descriere")));
        var properties = new ServiceSeedProperties(List.of(definition));
        when(serviceRepository.findByCode("new-code")).thenReturn(Optional.empty());

        seeder(properties).run();

        ArgumentCaptor<Service> captor = ArgumentCaptor.forClass(Service.class);
        verify(serviceRepository).save(captor.capture());
        Service saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("new-code");
        assertThat(saved.getDurationMinutes()).isEqualTo(30);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getTranslations()).hasSize(1);
        assertThat(saved.getTranslations().get(0).getName()).isEqualTo("Nume");
    }

    @Test
    void skipsExistingServiceMatchedByCode() {
        Service existing = new Service(20, true);
        existing.setCode("existing-code");
        var definition = new ServiceSeedProperties.ServiceDefinition("existing-code", 45, Map.of());
        var properties = new ServiceSeedProperties(List.of(definition));
        when(serviceRepository.findByCode("existing-code")).thenReturn(Optional.of(existing));

        seeder(properties).run();

        verify(serviceRepository, never()).save(existing);
        assertThat(existing.getDurationMinutes()).isEqualTo(20);
    }
}
