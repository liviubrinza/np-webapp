package com.brinza.notary.service;

import com.brinza.notary.config.AdminUserSeedProperties;
import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the initial admin-panel accounts from {@code admin-users.yml} on startup.
 * Unlike {@link ServiceSeeder}, this only ever creates missing accounts — an account
 * that already exists (including one whose password/role was since changed through the
 * Admin Users screen) is left untouched, since that screen is a live source of truth
 * this seeder must not clobber on every restart.
 */
@Component
@Order(2)
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final AdminUserSeedProperties properties;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserSeeder(AdminUserSeedProperties properties, AdminUserRepository adminUserRepository,
                            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (AdminUserSeedProperties.AdminUserDefinition definition : properties.adminUsers()) {
            if (adminUserRepository.findByUsername(definition.username()).isPresent()) {
                log.debug("Skipping username={}: account already exists", definition.username());
                continue;
            }
            adminUserRepository.save(new AdminUser(
                    definition.username(),
                    passwordEncoder.encode(definition.password()),
                    definition.role()));
            log.debug("Seeded admin user username={} role={}", definition.username(), definition.role());
        }
    }
}
