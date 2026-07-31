package com.brinza.notary.controller.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{lang:en|ro|hu}")
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(ContactController.class);

    private final String address;
    private final String phone;
    private final String email;
    private final String hours;
    private final double latitude;
    private final double longitude;

    public ContactController(
            @Value("${app.contact.address}") String address,
            @Value("${app.contact.phone}") String phone,
            @Value("${app.contact.email}") String email,
            @Value("${app.contact.hours}") String hours,
            @Value("${app.contact.latitude}") double latitude,
            @Value("${app.contact.longitude}") double longitude) {
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.hours = hours;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @GetMapping("/contact")
    public String showContact(Model model) {
        log.info("showContact called");
        model.addAttribute("address", address);
        model.addAttribute("phone", phone);
        model.addAttribute("email", email);
        model.addAttribute("hours", hours);
        model.addAttribute("latitude", latitude);
        model.addAttribute("longitude", longitude);
        return "public/contact";
    }
}
