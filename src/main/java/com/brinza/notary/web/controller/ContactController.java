package com.brinza.notary.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{lang:en|ro|hu}")
public class ContactController {

    private final String address;
    private final String phone;
    private final String email;
    private final double latitude;
    private final double longitude;

    public ContactController(
            @Value("${app.contact.address}") String address,
            @Value("${app.contact.phone}") String phone,
            @Value("${app.contact.email}") String email,
            @Value("${app.contact.latitude}") double latitude,
            @Value("${app.contact.longitude}") double longitude) {
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("address", address);
        model.addAttribute("phone", phone);
        model.addAttribute("email", email);
        model.addAttribute("latitude", latitude);
        model.addAttribute("longitude", longitude);
        return "public/contact";
    }
}
