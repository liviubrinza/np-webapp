package com.brinza.notary.service;

import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.dto.AdminUserForm;
import com.brinza.notary.dto.AdminUserView;
import com.brinza.notary.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@org.springframework.stereotype.Service
public class AdminUserManagementService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserManagementService.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserManagementService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminUserView> listAdmins() {
        return adminUserRepository.findAllByOrderByUsernameAsc().stream()
                .map(AdminUserManagementService::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserView getAdmin(Long id) {
        return toView(getAdminEntity(id));
    }

    @Transactional
    public void create(AdminUserForm form) {
        if (adminUserRepository.findByUsername(form.getUsername()).isPresent()) {
            log.debug("Rejected create: username={} already in use", form.getUsername());
            throw new IllegalArgumentException("Numele de utilizator este deja folosit.");
        }
        String rawPassword = validatePassword(form.getPassword(), true);
        AdminUser adminUser = new AdminUser(form.getUsername(), passwordEncoder.encode(rawPassword), form.getFullName(), form.getRole());
        adminUserRepository.save(adminUser);
        log.debug("Created admin user id={} username={} role={}", adminUser.getId(), adminUser.getUsername(), adminUser.getRole());
    }

    @Transactional
    public void update(Long id, AdminUserForm form) {
        AdminUser adminUser = getAdminEntity(id);
        adminUserRepository.findByUsername(form.getUsername())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Numele de utilizator este deja folosit.");
                });

        adminUser.setUsername(form.getUsername());
        adminUser.setFullName(form.getFullName());
        adminUser.setRole(form.getRole());
        String rawPassword = validatePassword(form.getPassword(), false);
        if (rawPassword != null) {
            adminUser.setPasswordHash(passwordEncoder.encode(rawPassword));
            log.debug("Password updated for admin user id={}", id);
        }
        log.debug("Updated admin user id={} username={} role={}", id, adminUser.getUsername(), adminUser.getRole());
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        AdminUser adminUser = getAdminEntity(id);
        if (adminUser.getUsername().equals(currentUsername)) {
            log.debug("Rejected delete: id={} is the currently logged-in user ({})", id, currentUsername);
            throw new IllegalArgumentException("Nu vă puteți șterge propriul cont.");
        }
        adminUserRepository.delete(adminUser);
    }

    private AdminUser getAdminEntity(Long id) {
        return adminUserRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No admin user with id " + id));
    }

    private static AdminUserView toView(AdminUser adminUser) {
        return new AdminUserView(adminUser.getId(), adminUser.getUsername(), adminUser.getFullName(), adminUser.getRole(),
                adminUser.getCreatedAt(), adminUser.getLastLogin());
    }

    private String validatePassword(String rawPassword, boolean required) {
        if (rawPassword == null || rawPassword.isBlank()) {
            if (required) {
                throw new IllegalArgumentException("Parola este obligatorie pentru un cont nou.");
            }
            return null;
        }
        if (rawPassword.length() < 4) {
            log.debug("Rejected password: shorter than minimum length");
            throw new IllegalArgumentException("Parola trebuie să aibă cel puțin 4 caractere.");
        }
        return rawPassword;
    }
}
