package com.brinza.notary.controller.web;

import com.brinza.notary.config.properties.ContactSettings;
import com.brinza.notary.service.StructuredDataService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the current locale and the request path with its /xx locale prefix stripped,
 * so the layout's language switcher can link to the same page under each locale. Also exposes
 * the configured site base URL (used to build absolute canonical/hreflang tags), a
 * BreadcrumbList JSON-LD block for pages with a known breadcrumb (null on the home page), and
 * the site's {@link ContactSettings} (null if unavailable) so the footer can show a NAP block
 * on every public page.
 *
 * <p>{@code @WebMvcTest} slices pick up every {@code @ControllerAdvice} bean in the application
 * regardless of its {@code basePackages} (see {@code AdminGlobalModelAttributes} for the same
 * gotcha on the admin side) - {@link ObjectProvider} defers both the {@link StructuredDataService}
 * and {@link ContactSettings} lookups to call time instead of constructor injection, so bean
 * creation never fails in slices that don't provide them; they just resolve to no breadcrumb / no
 * footer NAP block.
 */
@ControllerAdvice(basePackages = "com.brinza.notary.controller.web")
public class GlobalModelAttributes {

    private static final Logger log = LoggerFactory.getLogger(GlobalModelAttributes.class);

    private final String baseUrl;
    private final ObjectProvider<StructuredDataService> structuredDataService;
    private final ObjectProvider<ContactSettings> contactSettings;

    public GlobalModelAttributes(@Value("${app.base-url}") String baseUrl,
                                  ObjectProvider<StructuredDataService> structuredDataService,
                                  ObjectProvider<ContactSettings> contactSettings) {
        this.baseUrl = baseUrl;
        this.structuredDataService = structuredDataService;
        this.contactSettings = contactSettings;
    }

    @ModelAttribute
    public void addLocaleAttributes(HttpServletRequest request, Model model) {
        log.info("addLocaleAttributes called for requestURI={}", request.getRequestURI());
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String pathAfterLocale = path.length() > 3 ? path.substring(3) : "";
        model.addAttribute("currentLocale", LocaleContextHolder.getLocale().getLanguage());
        model.addAttribute("pathAfterLocale", pathAfterLocale);
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("contactSettings", contactSettings.getIfAvailable());

        StructuredDataService service = structuredDataService.getIfAvailable();
        String breadcrumbJsonLd = service != null
                ? service.breadcrumbJsonLd(pathAfterLocale, LocaleContextHolder.getLocale())
                : null;
        model.addAttribute("breadcrumbJsonLd", breadcrumbJsonLd);
    }
}
