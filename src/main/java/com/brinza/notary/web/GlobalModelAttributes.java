package com.brinza.notary.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the current locale and the request path with its /xx locale prefix stripped,
 * so the layout's language switcher can link to the same page under each locale.
 */
@ControllerAdvice(basePackages = "com.brinza.notary.web.controller")
public class GlobalModelAttributes {

    @ModelAttribute
    public void addLocaleAttributes(HttpServletRequest request, Model model) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String pathAfterLocale = path.length() > 3 ? path.substring(3) : "";
        model.addAttribute("currentLocale", LocaleContextHolder.getLocale().getLanguage());
        model.addAttribute("pathAfterLocale", pathAfterLocale);
    }
}
