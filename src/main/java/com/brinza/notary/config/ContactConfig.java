package com.brinza.notary.config;

import com.brinza.notary.config.properties.ContactSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ContactSettings.class)
public class ContactConfig {
}
