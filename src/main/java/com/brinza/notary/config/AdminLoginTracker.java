package com.brinza.notary.config;

import com.brinza.notary.repository.AdminUserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class AdminLoginTracker {

    private final AdminUserRepository adminUserRepository;

    public AdminLoginTracker(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (!(event.getAuthentication().getPrincipal() instanceof UserDetails userDetails)) {
            return;
        }
        adminUserRepository.findByUsername(userDetails.getUsername())
                .ifPresent(adminUser -> adminUser.setLastLogin(LocalDateTime.now()));
    }
}
