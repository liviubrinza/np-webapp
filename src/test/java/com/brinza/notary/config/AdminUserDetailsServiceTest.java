package com.brinza.notary.config;

import com.brinza.notary.domain.AdminRole;
import com.brinza.notary.domain.AdminUser;
import com.brinza.notary.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserDetailsServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    private AdminUserDetailsService service() {
        return new AdminUserDetailsService(adminUserRepository);
    }

    @Test
    void loadsUserWithRolePrefixedAuthority() {
        AdminUser adminUser = new AdminUser("titi", "hash", "Titi Full Name", AdminRole.TECHNICIAN);
        when(adminUserRepository.findByUsername("titi")).thenReturn(Optional.of(adminUser));

        UserDetails details = service().loadUserByUsername("titi");

        assertThat(details.getUsername()).isEqualTo("titi");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TECHNICIAN");
    }

    @Test
    void throwsWhenUsernameNotFound() {
        when(adminUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
