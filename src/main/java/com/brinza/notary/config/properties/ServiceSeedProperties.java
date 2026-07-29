package com.brinza.notary.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app")
public record ServiceSeedProperties(List<ServiceDefinition> services) {

    public record ServiceDefinition(String code, int durationMinutes, Map<String, Translation> translations) {
    }

    public record Translation(String name, String description) {
    }
}
