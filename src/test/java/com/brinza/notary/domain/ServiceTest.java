package com.brinza.notary.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceTest {

    @Test
    void addTranslationSetsBackReference() {
        Service service = new Service(30, true);
        ServiceTranslation translation = new ServiceTranslation("en", "Name", "Description");

        service.addTranslation(translation);

        assertThat(service.getTranslations()).containsExactly(translation);
        assertThat(translation.getService()).isSameAs(service);
    }

    @Test
    void removeTranslationClearsBackReference() {
        Service service = new Service(30, true);
        ServiceTranslation translation = new ServiceTranslation("en", "Name", "Description");
        service.addTranslation(translation);

        service.removeTranslation(translation);

        assertThat(service.getTranslations()).isEmpty();
        assertThat(translation.getService()).isNull();
    }
}
