package com.kienlongbank.apigateway.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.result.view.RedirectView;

@Controller
public class SwaggerController {

    @GetMapping("/")
    public RedirectView index() {
        return new RedirectView("/swagger-ui.html");
    }

    @GetMapping("/swagger")
    public RedirectView swagger() {
        return new RedirectView("/swagger-ui.html");
    }
} 