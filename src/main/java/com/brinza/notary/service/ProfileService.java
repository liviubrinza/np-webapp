package com.brinza.notary.service;

import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@org.springframework.stereotype.Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public String getFullName(String username) {
        return adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("No admin user with username " + username))
                .getFullName();
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        // Never log raw passwords, only that a change was attempted/succeeded.
        log.info("changePassword called for username={}", username);
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("No admin user with username " + username));

        if (!passwordEncoder.matches(currentPassword, adminUser.getPasswordHash())) {
            log.debug("Rejected changePassword for username={}: current password mismatch", username);
            throw new IllegalArgumentException("Parola curentă este incorectă.");
        }
        adminUser.setPasswordHash(passwordEncoder.encode(newPassword));
        log.debug("Password updated for username={}", username);
    }
}
