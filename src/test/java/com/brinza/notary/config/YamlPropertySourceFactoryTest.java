package com.brinza.notary.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;

import static org.assertj.core.api.Assertions.assertThat;

class YamlPropertySourceFactoryTest {

    private final YamlPropertySourceFactory factory = new YamlPropertySourceFactory();

    @Test
    void loadsYamlIntoPropertiesPropertySource() throws Exception {
        EncodedResource resource = new EncodedResource(new ClassPathResource("fixtures/yaml-property-source-factory-test.yml"));

        PropertySource<?> propertySource = factory.createPropertySource("test-source", resource);

        assertThat(propertySource.getName()).isEqualTo("test-source");
        assertThat(propertySource.getProperty("sample.foo")).isEqualTo("bar");
        assertThat(propertySource.getProperty("sample.count")).isEqualTo(3);
    }

    @Test
    void usesResourceFilenameWhenNameIsNull() throws Exception {
        EncodedResource resource = new EncodedResource(new ClassPathResource("fixtures/yaml-property-source-factory-test.yml"));

        PropertySource<?> propertySource = factory.createPropertySource(null, resource);

        assertThat(propertySource.getName()).isEqualTo("yaml-property-source-factory-test.yml");
    }
}
