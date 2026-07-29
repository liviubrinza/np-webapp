package com.brinza.notary.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Exposes {@code app.contact.phone} as a named bean so it can be referenced directly from
 * templates that aren't backed by a controller of our own (e.g. {@code error.html}, rendered
 * by Spring Boot's built-in error-handling machinery) via {@code ${@contactProperties.phone}}.
 */
@Component
public class ContactProperties {

    private final String phone;

    public ContactProperties(@Value("${app.contact.phone}") String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }
}
