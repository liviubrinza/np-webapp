package com.brinza.notary.service;

import com.brinza.notary.domain.AdminRole;
import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.dto.AdminUserForm;
import com.brinza.notary.dto.AdminUserView;
import com.brinza.notary.repository.AdminUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@org.springframework.stereotype.Service
public class AdminUserManagementService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserManagementService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminUserView> listAdmins() {
        return adminUserRepository.findAllByRoleOrderByUsernameAsc(AdminRole.ADMIN).stream()
                .map(u -> new AdminUserView(u.getId(), u.getUsername(), u.getCreatedAt(), u.getLastLogin()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserView getAdmin(Long id) {
        AdminUser adminUser = getAdminEntity(id);
        return new AdminUserView(adminUser.getId(), adminUser.getUsername(), adminUser.getCreatedAt(), adminUser.getLastLogin());
    }

    @Transactional
    public void create(AdminUserForm form) {
        if (adminUserRepository.findByUsername(form.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Numele de utilizator este deja folosit.");
        }
        String rawPassword = validatePassword(form.getPassword(), true);
        AdminUser adminUser = new AdminUser(form.getUsername(), passwordEncoder.encode(rawPassword), AdminRole.ADMIN);
        adminUserRepository.save(adminUser);
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
        String rawPassword = validatePassword(form.getPassword(), false);
        if (rawPassword != null) {
            adminUser.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
    }

    @Transactional
    public void delete(Long id) {
        adminUserRepository.delete(getAdminEntity(id));
    }

    private AdminUser getAdminEntity(Long id) {
        AdminUser adminUser = adminUserRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No admin user with id " + id));
        if (adminUser.getRole() != AdminRole.ADMIN) {
            throw new NoSuchElementException("No admin user with id " + id);
        }
        return adminUser;
    }

    private String validatePassword(String rawPassword, boolean required) {
        if (rawPassword == null || rawPassword.isBlank()) {
            if (required) {
                throw new IllegalArgumentException("Parola este obligatorie pentru un cont nou.");
            }
            return null;
        }
        if (rawPassword.length() < 4) {
            throw new IllegalArgumentException("Parola trebuie să aibă cel puțin 4 caractere.");
        }
        return rawPassword;
    }
}
