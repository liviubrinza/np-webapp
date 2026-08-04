package com.brinza.notary.service;

import com.brinza.notary.config.properties.ContactSettings;
import com.brinza.notary.dto.ServiceView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds schema.org JSON-LD structured data (LegalService, Service catalog, BreadcrumbList) as
 * pre-serialized JSON strings. Templates embed them via Thymeleaf's {@code /*[( )]*}{@code /}
 * script-inlining idiom (see {@code layout/fragments.html}, {@code public/contact.html},
 * {@code public/services.html}) rather than {@code th:utext}, which the pr-security-tests.yml
 * Thymeleaf guard disallows.
 */
@org.springframework.stereotype.Service
public class StructuredDataService {

    private static final Logger log = LoggerFactory.getLogger(StructuredDataService.class);

    private static final Map<String, String> BREADCRUMB_LABEL_KEYS = Map.of(
            "/services", "nav.services",
            "/contact", "contact.heading",
            "/book", "nav.book");

    private final JsonMapper jsonMapper;
    private final MessageSource messageSource;
    private final ContactSettings contactSettings;
    private final String baseUrl;

    public StructuredDataService(JsonMapper jsonMapper, MessageSource messageSource,
                                  ContactSettings contactSettings, @Value("${app.base-url}") String baseUrl) {
        this.jsonMapper = jsonMapper;
        this.messageSource = messageSource;
        this.contactSettings = contactSettings;
        this.baseUrl = baseUrl;
    }

    public String legalServiceJsonLd(Locale locale) {
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("@type", "PostalAddress");
        address.put("streetAddress", contactSettings.street());
        address.put("addressLocality", contactSettings.city());
        address.put("postalCode", contactSettings.postalCode());
        address.put("addressCountry", contactSettings.countryCode());

        Map<String, Object> geo = new LinkedHashMap<>();
        geo.put("@type", "GeoCoordinates");
        geo.put("latitude", contactSettings.latitude());
        geo.put("longitude", contactSettings.longitude());

        Map<String, Object> openingHours = new LinkedHashMap<>();
        openingHours.put("@type", "OpeningHoursSpecification");
        openingHours.put("dayOfWeek", contactSettings.daysOfWeek());
        openingHours.put("opens", contactSettings.openingTime());
        openingHours.put("closes", contactSettings.closingTime());

        Map<String, Object> legalService = new LinkedHashMap<>();
        legalService.put("@context", "https://schema.org");
        legalService.put("@type", "LegalService");
        legalService.put("name", messageSource.getMessage("site.title", null, locale));
        legalService.put("telephone", contactSettings.phone());
        legalService.put("email", contactSettings.email());
        legalService.put("url", pageUrl(locale, "/contact"));
        legalService.put("address", address);
        legalService.put("geo", geo);
        legalService.put("openingHoursSpecification", openingHours);

        return toJson(legalService);
    }

    public String servicesJsonLd(List<ServiceView> services, Locale locale) {
        String providerName = messageSource.getMessage("site.title", null, locale);
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (ServiceView serviceView : services) {
            Map<String, Object> provider = new LinkedHashMap<>();
            provider.put("@type", "LegalService");
            provider.put("name", providerName);

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("@type", "Service");
            node.put("name", serviceView.name());
            node.put("description", serviceView.description());
            node.put("provider", provider);
            nodes.add(node);
        }

        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("@context", "https://schema.org");
        graph.put("@graph", nodes);
        return toJson(graph);
    }

    /**
     * Returns {@code null} for paths with no known breadcrumb (e.g. the home page itself), so
     * callers can skip embedding the {@code <script>} block entirely.
     */
    public String breadcrumbJsonLd(String pathAfterLocale, Locale locale) {
        boolean isConfirmation = "/book/confirmation".equals(pathAfterLocale);
        String labelKey = BREADCRUMB_LABEL_KEYS.get(pathAfterLocale);
        if (labelKey == null && !isConfirmation) {
            return null;
        }

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(listItem(1, messageSource.getMessage("nav.home", null, locale), pageUrl(locale, "")));
        if (isConfirmation) {
            items.add(listItem(2, messageSource.getMessage("nav.book", null, locale), pageUrl(locale, "/book")));
            items.add(listItem(3, messageSource.getMessage("book.confirmation.title", null, locale), pageUrl(locale, pathAfterLocale)));
        } else {
            items.add(listItem(2, messageSource.getMessage(labelKey, null, locale), pageUrl(locale, pathAfterLocale)));
        }

        Map<String, Object> breadcrumb = new LinkedHashMap<>();
        breadcrumb.put("@context", "https://schema.org");
        breadcrumb.put("@type", "BreadcrumbList");
        breadcrumb.put("itemListElement", items);
        return toJson(breadcrumb);
    }

    private Map<String, Object> listItem(int position, String name, String url) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("@type", "ListItem");
        item.put("position", position);
        item.put("name", name);
        item.put("item", url);
        return item;
    }

    private String pageUrl(Locale locale, String pathAfterLocale) {
        return baseUrl + "/" + locale.getLanguage() + pathAfterLocale;
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            log.error("Failed to serialize structured data", e);
            return "{}";
        }
    }
}
