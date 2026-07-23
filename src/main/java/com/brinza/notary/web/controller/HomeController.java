package com.brinza.notary.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{lang:en|ro|hu}")
public class HomeController {

    @GetMapping({"", "/"})
    public String home() {
        return "public/home";
    }
}
