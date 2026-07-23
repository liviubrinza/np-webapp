package com.brinza.notary.service;

import com.brinza.notary.domain.Service;
import com.brinza.notary.domain.ServiceTranslation;
import com.brinza.notary.dto.ServiceView;
import com.brinza.notary.repository.ServiceRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Note: named ServiceCatalogService, not ServiceService, to avoid a same-simple-name
 * import clash between the {@link Service} entity and {@code org.springframework.stereotype.Service}
 * in classes that need both.
 */
@org.springframework.stereotype.Service
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;

    public ServiceCatalogService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceView> findActiveServices(Locale locale) {
        return serviceRepository.findByActiveTrue().stream()
                .map(service -> toView(service, locale))
                .toList();
    }

    private ServiceView toView(Service service, Locale locale) {
        ServiceTranslation translation = resolveTranslation(service, locale);
        return new ServiceView(
                service.getId(),
                translation.getName(),
                translation.getDescription(),
                service.getDurationMinutes()
        );
    }

    /**
     * Must be called within an active transaction if the given {@link Service} was loaded
     * outside the current one, since {@code translations} is lazy-loaded.
     */
    public String resolveName(Service service, Locale locale) {
        return resolveTranslation(service, locale).getName();
    }

    private ServiceTranslation resolveTranslation(Service service, Locale locale) {
        String language = locale.getLanguage();
        return service.getTranslations().stream()
                .filter(t -> t.getLocale().equals(language))
                .findFirst()
                .or(() -> service.getTranslations().stream()
                        .filter(t -> t.getLocale().equals("en"))
                        .findFirst())
                .orElseThrow(() -> new IllegalStateException(
                        "Service " + service.getId() + " has no translation for '" + language + "' or fallback 'en'"));
    }
}
