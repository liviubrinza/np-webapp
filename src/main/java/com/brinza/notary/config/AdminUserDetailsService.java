package com.brinza.notary.config;

import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AdminUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserDetailsService.class);

    private final AdminUserRepository adminUserRepository;
    private final LoginAttemptService loginAttemptService;

    public AdminUserDetailsService(AdminUserRepository adminUserRepository, LoginAttemptService loginAttemptService) {
        this.adminUserRepository = adminUserRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.debug("No AdminUser found for username={}", username);
                    return new UsernameNotFoundException("Unknown admin user: " + username);
                });

        boolean locked = loginAttemptService.isLocked(username);
        log.debug("AdminUser found for username={}, role={}, locked={}", username, adminUser.getRole(), locked);
        return User.withUsername(adminUser.getUsername())
                .password(adminUser.getPasswordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + adminUser.getRole().name()))
                .accountLocked(locked)
                .build();
    }
}
