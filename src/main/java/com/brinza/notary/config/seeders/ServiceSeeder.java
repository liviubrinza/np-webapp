package com.brinza.notary.config.seeders;

import com.brinza.notary.config.properties.ServiceSeedProperties;
import com.brinza.notary.domain.Service;
import com.brinza.notary.domain.ServiceTranslation;
import com.brinza.notary.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Creates the initial services from {@code services.yml} on startup, matched by their
 * stable {@code code}. Unlike its previous behavior, this only ever creates missing
 * services — an existing one (including one since edited through the admin Services
 * screen) is left untouched, since that screen is now a live source of truth this
 * seeder must not clobber on every restart. Mirrors {@link AdminUserSeeder}'s approach
 * to the same problem.
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
        for (ServiceSeedProperties.ServiceDefinition definition : properties.services()) {
            if (serviceRepository.findByCode(definition.code()).isPresent()) {
                log.debug("Skipping service code={}: already exists", definition.code());
                continue;
            }
            Service service = new Service(definition.durationMinutes(), true);
            service.setCode(definition.code());
            for (Map.Entry<String, ServiceSeedProperties.Translation> entry : definition.translations().entrySet()) {
                ServiceSeedProperties.Translation translation = entry.getValue();
                service.addTranslation(new ServiceTranslation(entry.getKey(), translation.name(), translation.description()));
            }
            serviceRepository.save(service);
            log.debug("Seeded service code={}", definition.code());
        }
    }
}
