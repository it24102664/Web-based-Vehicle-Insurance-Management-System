package com.example.Insurance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    // REMOVE THIS - IT CONFLICTS WITH AuthController
    // @GetMapping("/dashboard")          ← DELETE THIS LINE
    // public String dashboard() {        ← DELETE THIS LINE
    //     return "dashboard";            ← DELETE THIS LINE
    // }                                  ← DELETE THIS LINE

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/services")
    public String services() {
        return "services";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }
}
