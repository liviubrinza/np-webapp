package com.brinza.notary.service;

import com.brinza.notary.config.properties.ContactSettings;
import com.brinza.notary.dto.ServiceView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StructuredDataServiceTest {

    private static final Locale RO = Locale.of("ro");

    @Mock
    private MessageSource messageSource;

    private final ContactSettings contactSettings = new ContactSettings(
            "Str. Test 1", "Test City", "County", "111111", "RO",
            "0700000000", "office@example.com", "09:00", "17:00",
            List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"),
            46.0, 23.0);

    private StructuredDataService service() {
        return new StructuredDataService(JsonMapper.builder().build(), messageSource, contactSettings,
                "http://localhost:8080");
    }

    private void stubMessage(String key, String value) {
        when(messageSource.getMessage(eq(key), any(), eq(RO))).thenReturn(value);
    }

    @Test
    void legalServiceJsonLdIncludesAddressGeoAndOpeningHours() {
        stubMessage("site.title", "Birou Notarial");

        String json = service().legalServiceJsonLd(RO);

        assertThat(json).contains("\"@context\":\"https://schema.org\"")
                .contains("\"@type\":\"LegalService\"")
                .contains("\"name\":\"Birou Notarial\"")
                .contains("\"telephone\":\"0700000000\"")
                .contains("\"email\":\"office@example.com\"")
                .contains("\"url\":\"http://localhost:8080/ro/contact\"")
                .contains("\"@type\":\"PostalAddress\"")
                .contains("\"streetAddress\":\"Str. Test 1\"")
                .contains("\"addressLocality\":\"Test City\"")
                .contains("\"addressLocality\":\"Test City\"")
                .contains("\"postalCode\":\"111111\"")
                .contains("\"addressCountry\":\"RO\"")
                .contains("\"@type\":\"GeoCoordinates\"")
                .contains("\"latitude\":46.0")
                .contains("\"longitude\":23.0")
                .contains("\"@type\":\"OpeningHoursSpecification\"")
                .contains("\"dayOfWeek\":[\"Monday\",\"Tuesday\",\"Wednesday\",\"Thursday\",\"Friday\"]")
                .contains("\"opens\":\"09:00\"")
                .contains("\"closes\":\"17:00\"");
    }

    @Test
    void servicesJsonLdListsEachServiceWithProvider() {
        stubMessage("site.title", "Birou Notarial");
        List<ServiceView> services = List.of(
                new ServiceView(1L, "Autentificare", "descriere1", 30),
                new ServiceView(2L, "Legalizare copie", "descriere2", 15));

        String json = service().servicesJsonLd(services, RO);

        assertThat(json).contains("\"@context\":\"https://schema.org\"")
                .contains("\"@graph\":[")
                .contains("\"@type\":\"Service\"")
                .contains("\"name\":\"Autentificare\"")
                .contains("\"description\":\"descriere1\"")
                .contains("\"name\":\"Legalizare copie\"")
                .contains("\"description\":\"descriere2\"")
                .contains("\"provider\":{\"@type\":\"LegalService\",\"name\":\"Birou Notarial\"}");
    }

    @Test
    void servicesJsonLdWithNoServicesIsAnEmptyGraph() {
        String json = service().servicesJsonLd(List.of(), RO);

        assertThat(json).contains("\"@graph\":[]");
    }

    @Test
    void breadcrumbJsonLdIsNullOnHomePage() {
        assertThat(service().breadcrumbJsonLd("", RO)).isNull();
    }

    @Test
    void breadcrumbJsonLdIsNullForUnknownPath() {
        assertThat(service().breadcrumbJsonLd("/unknown", RO)).isNull();
    }

    @Test
    void breadcrumbJsonLdHasTwoLevelsForServicesPage() {
        stubMessage("nav.home", "Acasă");
        stubMessage("nav.services", "Servicii");

        String json = service().breadcrumbJsonLd("/services", RO);

        assertThat(json).contains("\"@type\":\"BreadcrumbList\"")
                .contains("\"position\":1")
                .contains("\"name\":\"Acasă\"")
                .contains("\"item\":\"http://localhost:8080/ro\"")
                .contains("\"position\":2")
                .contains("\"name\":\"Servicii\"")
                .contains("\"item\":\"http://localhost:8080/ro/services\"")
                .doesNotContain("\"position\":3");
    }

    @Test
    void breadcrumbJsonLdHasThreeLevelsForBookingConfirmation() {
        stubMessage("nav.home", "Acasă");
        stubMessage("nav.book", "Programează-te");
        stubMessage("book.confirmation.title", "Programare înregistrată");

        String json = service().breadcrumbJsonLd("/book/confirmation", RO);

        assertThat(json).contains("\"position\":1")
                .contains("\"position\":2")
                .contains("\"name\":\"Programează-te\"")
                .contains("\"item\":\"http://localhost:8080/ro/book\"")
                .contains("\"position\":3")
                .contains("\"name\":\"Programare înregistrată\"")
                .contains("\"item\":\"http://localhost:8080/ro/book/confirmation\"");
    }
}
