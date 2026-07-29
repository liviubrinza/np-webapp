package com.brinza.notary.config;

import com.brinza.notary.config.properties.AppointmentSeedProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:appointments.yml", factory = YamlPropertySourceFactory.class)
@EnableConfigurationProperties(AppointmentSeedProperties.class)
public class AppointmentSeedConfig {
}
