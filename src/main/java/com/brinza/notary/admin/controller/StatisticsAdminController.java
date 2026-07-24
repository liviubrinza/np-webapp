package com.brinza.notary.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/statistics")
public class StatisticsAdminController {

    @GetMapping
    public String requests() {
        return "admin/statistics/requests";
    }

    @GetMapping("/traffic")
    public String traffic() {
        return "admin/statistics/traffic";
    }

    @GetMapping("/activity")
    public String activity() {
        return "admin/statistics/activity";
    }
}
