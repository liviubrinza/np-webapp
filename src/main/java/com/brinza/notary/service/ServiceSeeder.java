package com.brinza.notary.service;

import com.brinza.notary.config.ServiceSeedProperties;
import com.brinza.notary.domain.Service;
import com.brinza.notary.domain.ServiceTranslation;
import com.brinza.notary.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
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
 *
 * <p>Ordered ahead of other seeders ({@link AppointmentDemoDataSeeder}) that need
 * services to already exist.
 */
@Component
@Order(1)
public class ServiceSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ServiceSeeder.class);

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
            boolean isNew = serviceRepository.findByCode(definition.code()).isEmpty();
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
            log.debug("{} service code={}", isNew ? "Created" : "Updated", definition.code());
        }

        for (Service service : serviceRepository.findAll()) {
            if (!codesInYaml.contains(service.getCode())) {
                service.setActive(false);
                log.debug("Deactivated service code={} (no longer present in services.yml)", service.getCode());
            }
        }
    }

    private void syncTranslations(Service service, Map<String, ServiceSeedProperties.Translation> translations) {
        log.debug("syncTranslations called for serviceCode={} locales={}", service.getCode(), translations.keySet());
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
