package com.brinza.notary.service;

import com.brinza.notary.config.ServiceSeedProperties;
import com.brinza.notary.domain.Service;
import com.brinza.notary.domain.ServiceTranslation;
import com.brinza.notary.repository.ServiceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Syncs the {@code services} table to {@code services.yml} on every startup: services
 * are matched by their stable {@code code}, not database id, so entries can be freely
 * reordered/added in the YAML. A service removed from the YAML is deactivated rather
 * than deleted, since existing appointments may still reference it.
 */
@Component
public class ServiceSeeder implements CommandLineRunner {

    private final ServiceSeedProperties properties;
    private final ServiceRepository serviceRepository;

    public ServiceSeeder(ServiceSeedProperties properties, ServiceRepository serviceRepository) {
        this.properties = properties;
        this.serviceRepository = serviceRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Set<String> codesInYaml = new HashSet<>();

        for (ServiceSeedProperties.ServiceDefinition definition : properties.services()) {
            codesInYaml.add(definition.code());
            Service service = serviceRepository.findByCode(definition.code())
                    .orElseGet(() -> {
                        Service created = new Service(definition.durationMinutes(), true);
                        created.setCode(definition.code());
                        return created;
                    });
            service.setDurationMinutes(definition.durationMinutes());
            service.setActive(true);
            syncTranslations(service, definition.translations());
            serviceRepository.save(service);
        }

        for (Service service : serviceRepository.findAll()) {
            if (!codesInYaml.contains(service.getCode())) {
                service.setActive(false);
            }
        }
    }

    private void syncTranslations(Service service, Map<String, ServiceSeedProperties.Translation> translations) {
        translations.forEach((locale, translation) -> {
            ServiceTranslation existing = service.getTranslations().stream()
                    .filter(t -> t.getLocale().equals(locale))
                    .findFirst()
                    .orElse(null);
            if (existing == null) {
                service.addTranslation(new ServiceTranslation(locale, translation.name(), translation.description()));
            } else {
                existing.setName(translation.name());
                existing.setDescription(translation.description());
            }
        });
    }
}
