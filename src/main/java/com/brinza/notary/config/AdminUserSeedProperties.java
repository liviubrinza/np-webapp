package com.brinza.notary.config;

import com.brinza.notary.domain.AdminRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AdminUserSeedProperties(List<AdminUserDefinition> adminUsers) {

    public record AdminUserDefinition(String username, String password, AdminRole role) {
    }
}
