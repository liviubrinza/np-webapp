package com.brinza.notary.controller.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the current locale and the request path with its /xx locale prefix stripped,
 * so the layout's language switcher can link to the same page under each locale.
 */
@ControllerAdvice(basePackages = "com.brinza.notary.controller.web")
public class GlobalModelAttributes {

    private static final Logger log = LoggerFactory.getLogger(GlobalModelAttributes.class);

    @ModelAttribute
    public void addLocaleAttributes(HttpServletRequest request, Model model) {
        log.info("addLocaleAttributes called for requestURI={}", request.getRequestURI());
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String pathAfterLocale = path.length() > 3 ? path.substring(3) : "";
        model.addAttribute("currentLocale", LocaleContextHolder.getLocale().getLanguage());
        model.addAttribute("pathAfterLocale", pathAfterLocale);
    }
}
