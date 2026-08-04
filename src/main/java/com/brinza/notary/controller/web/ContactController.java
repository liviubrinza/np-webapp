package com.brinza.notary.controller.web;

import com.brinza.notary.config.properties.ContactSettings;
import com.brinza.notary.service.StructuredDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{lang:en|ro|hu}")
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(ContactController.class);

    private final ContactSettings contactSettings;
    private final StructuredDataService structuredDataService;

    public ContactController(ContactSettings contactSettings, StructuredDataService structuredDataService) {
        this.contactSettings = contactSettings;
        this.structuredDataService = structuredDataService;
    }

    @GetMapping("/contact")
    public String showContact(Model model) {
        log.info("showContact called");
        model.addAttribute("address", contactSettings.displayAddress());
        model.addAttribute("phone", contactSettings.phone());
        model.addAttribute("email", contactSettings.email());
        model.addAttribute("hours", contactSettings.displayHours());
        model.addAttribute("latitude", contactSettings.latitude());
        model.addAttribute("longitude", contactSettings.longitude());
        model.addAttribute("legalServiceJsonLd", structuredDataService.legalServiceJsonLd(LocaleContextHolder.getLocale()));
        return "public/contact";
    }
}
