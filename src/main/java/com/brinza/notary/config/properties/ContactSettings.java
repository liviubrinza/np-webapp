package com.brinza.notary.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Structured {@code app.contact.*} settings — backs both the Contact page display (via
 * {@link #displayAddress()}/{@link #displayHours()}) and the LegalService/OpeningHoursSpecification
 * structured data built by {@code StructuredDataService}. {@code daysOfWeek} holds schema.org's
 * canonical English day tokens (e.g. {@code "Monday"}); it is not localized and must be kept in
 * sync by hand with the separately translated {@code contact.schedule.days} display label in
 * {@code messages_*.properties}.
 */
@ConfigurationProperties(prefix = "app.contact")
public record ContactSettings(
        String street,
        String city,
        String postalCode,
        String countryCode,
        String phone,
        String email,
        String openingTime,
        String closingTime,
        List<String> daysOfWeek,
        double latitude,
        double longitude) {

    public String displayAddress() {
        return street + ", " + city + " " + postalCode;
    }

    public String displayHours() {
        return openingTime + " - " + closingTime;
    }
}
