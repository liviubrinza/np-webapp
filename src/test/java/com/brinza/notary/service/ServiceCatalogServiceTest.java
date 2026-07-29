package com.brinza.notary.service;

import com.brinza.notary.domain.Service;
import com.brinza.notary.domain.ServiceTranslation;
import com.brinza.notary.dto.ServiceView;
import com.brinza.notary.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    private final ServiceCatalogService service() {
        return new ServiceCatalogService(serviceRepository);
    }

    @Test
    void findActiveServicesMapsToViewsInRequestedLocale() {
        Service notaryService = new Service(30, true);
        notaryService.addTranslation(new ServiceTranslation("ro", "Autentificare", "descriere"));
        notaryService.addTranslation(new ServiceTranslation("en", "Authentication", "description"));
        when(serviceRepository.findByActiveTrue()).thenReturn(List.of(notaryService));

        List<ServiceView> views = service().findActiveServices(Locale.of("en"));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).name()).isEqualTo("Authentication");
        assertThat(views.get(0).durationMinutes()).isEqualTo(30);
    }

    @Test
    void resolveNameFallsBackToEnglishWhenLocaleMissing() {
        Service notaryService = new Service(30, true);
        notaryService.addTranslation(new ServiceTranslation("en", "Authentication", "description"));

        String name = service().resolveName(notaryService, Locale.of("hu"));

        assertThat(name).isEqualTo("Authentication");
    }

    @Test
    void resolveNameThrowsWhenNoLocaleAndNoFallback() {
        Service notaryService = new Service(30, true);
        notaryService.addTranslation(new ServiceTranslation("ro", "Autentificare", "descriere"));

        assertThatThrownBy(() -> service().resolveName(notaryService, Locale.of("hu")))
                .isInstanceOf(IllegalStateException.class);
    }
}
