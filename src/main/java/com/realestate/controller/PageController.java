package com.realestate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping({"/src/main/resources/static/index.html", "/src/main/resources/static/"})
    public String redirectSourcePage() {
        return "redirect:/";
    }
}
