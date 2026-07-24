package com.brinza.notary.service;

import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.repository.AdminUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@org.springframework.stereotype.Service
public class ProfileService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("No admin user with username " + username));

        if (!passwordEncoder.matches(currentPassword, adminUser.getPasswordHash())) {
            throw new IllegalArgumentException("Parola curentă este incorectă.");
        }
        adminUser.setPasswordHash(passwordEncoder.encode(newPassword));
    }
}
