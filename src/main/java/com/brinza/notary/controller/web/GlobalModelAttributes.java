package com.brinza.notary.controller.web;

import com.brinza.notary.config.SystemSettings;
import com.brinza.notary.config.properties.ContactSettings;
import com.brinza.notary.service.StructuredDataService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Exposes the current locale and the request path with its /xx locale prefix stripped,
 * so the layout's language switcher can link to the same page under each locale. Also exposes
 * the configured site base URL (used to build absolute canonical/hreflang tags), a
 * BreadcrumbList JSON-LD block for pages with a known breadcrumb (null on the home page), and
 * the site's {@link ContactSettings} (null if unavailable) so the footer can show a NAP block
 * on every public page, and the admin-configurable maintenance/notice banner state - either a
 * custom typed message, or (if none was typed but a vacation period was selected) a translated
 * "office is on holiday" message built via {@link MessageSource} for the visitor's current locale.
 * A vacation range also gets exposed in plain ISO form ({@code notificationVacationStartIso}/
 * {@code notificationVacationEndIso}), which {@code book.html}'s date picker uses to grey out
 * those days so a visitor can't request an appointment during the announced closure.
 *
 * <p>{@code @WebMvcTest} slices pick up every {@code @ControllerAdvice} bean in the application
 * regardless of its {@code basePackages} (see {@code AdminGlobalModelAttributes} for the same
 * gotcha on the admin side) - {@link ObjectProvider} defers the {@link StructuredDataService},
 * {@link ContactSettings} and {@link SystemSettings} lookups to call time instead of constructor
 * injection, so bean creation never fails in slices that don't provide them; they just resolve to
 * no breadcrumb / no footer NAP block / no notification banner. {@link MessageSource} is a plain
 * constructor dependency (not an {@link ObjectProvider}) since Boot auto-configures it in every
 * slice that renders Thymeleaf's own {@code #{...}} i18n messages, which every affected test here
 * already relies on.
 */
@ControllerAdvice(basePackages = "com.brinza.notary.controller.web")
public class GlobalModelAttributes {

    private static final Logger log = LoggerFactory.getLogger(GlobalModelAttributes.class);

    private final String baseUrl;
    private final ObjectProvider<StructuredDataService> structuredDataService;
    private final ObjectProvider<ContactSettings> contactSettings;
    private final ObjectProvider<SystemSettings> systemSettings;
    private final MessageSource messageSource;

    public GlobalModelAttributes(@Value("${app.base-url}") String baseUrl,
                                  ObjectProvider<StructuredDataService> structuredDataService,
                                  ObjectProvider<ContactSettings> contactSettings,
                                  ObjectProvider<SystemSettings> systemSettings,
                                  MessageSource messageSource) {
        this.baseUrl = baseUrl;
        this.structuredDataService = structuredDataService;
        this.contactSettings = contactSettings;
        this.systemSettings = systemSettings;
        this.messageSource = messageSource;
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

        addNotificationBannerAttributes(model, systemSettings.getIfAvailable());
    }

    /**
     * A custom typed message always wins and is rendered as plain text; a vacation period with no
     * custom message falls back to a translated "office is closed" template, split into
     * prefix/between/suffix pieces so the template can wrap just the date(s) in {@code <strong>}
     * without resorting to unescaped HTML output (blocked by the CI Thymeleaf guard). A single-day
     * period (start equals end) uses a dedicated "closed on <date>" template instead of "closed
     * between <date> and <date>" - see {@code notification.vacation.message.singleDay.*} keys.
     */
    private void addNotificationBannerAttributes(Model model, SystemSettings settings) {
        boolean enabled = settings != null && settings.isNotificationEnabled();
        String customMessage = settings != null ? settings.getNotificationMessage() : null;
        boolean hasCustomMessage = customMessage != null && !customMessage.isBlank();
        LocalDate vacationStart = settings != null ? settings.getNotificationVacationStart() : null;
        LocalDate vacationEnd = settings != null ? settings.getNotificationVacationEnd() : null;
        boolean hasVacationRange = vacationStart != null && vacationEnd != null;

        boolean show = enabled && (hasCustomMessage || hasVacationRange);
        boolean vacationActive = show && !hasCustomMessage;

        model.addAttribute("notificationEnabled", show);
        model.addAttribute("notificationVacationActive", vacationActive);
        model.addAttribute("notificationMessage", hasCustomMessage ? customMessage : null);

        if (vacationActive) {
            boolean singleDay = vacationStart.isEqual(vacationEnd);
            Locale locale = LocaleContextHolder.getLocale();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", locale);
            String prefixKey = singleDay
                    ? "notification.vacation.message.singleDay.prefix"
                    : "notification.vacation.message.prefix";
            String suffixKey = singleDay
                    ? "notification.vacation.message.singleDay.suffix"
                    : "notification.vacation.message.suffix";

            model.addAttribute("notificationVacationSingleDay", singleDay);
            model.addAttribute("notificationVacationPrefix", messageSource.getMessage(prefixKey, null, locale));
            model.addAttribute("notificationVacationSuffix", messageSource.getMessage(suffixKey, null, locale));
            model.addAttribute("notificationVacationStartDisplay", vacationStart.format(formatter));
            // ISO (yyyy-MM-dd) form of the same range, for the booking page's date picker to grey
            // out - the locale-formatted *Display strings above are for human reading only.
            model.addAttribute("notificationVacationStartIso", vacationStart.toString());
            model.addAttribute("notificationVacationEndIso", vacationEnd.toString());
            if (!singleDay) {
                model.addAttribute("notificationVacationBetween",
                        messageSource.getMessage("notification.vacation.message.between", null, locale));
                model.addAttribute("notificationVacationEndDisplay", vacationEnd.format(formatter));
            }
        }
    }
}
