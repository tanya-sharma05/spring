package com.example.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomePageController {

    @RequestMapping("/")
    public String greet() {
        return "Welcome to our page!";
    }

    @RequestMapping("/about")
    public String about() {
        return "About page!";
    }
}
