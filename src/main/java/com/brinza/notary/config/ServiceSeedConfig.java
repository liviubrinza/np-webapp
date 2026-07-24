package com.brinza.notary.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:services.yml", factory = YamlPropertySourceFactory.class)
@EnableConfigurationProperties(ServiceSeedProperties.class)
public class ServiceSeedConfig {
}
