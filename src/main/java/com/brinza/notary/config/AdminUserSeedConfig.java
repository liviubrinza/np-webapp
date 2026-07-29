package com.brinza.notary.config;

import com.brinza.notary.config.properties.AdminUserSeedProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:admin-users.yml", factory = YamlPropertySourceFactory.class)
@EnableConfigurationProperties(AdminUserSeedProperties.class)
public class AdminUserSeedConfig {
}
