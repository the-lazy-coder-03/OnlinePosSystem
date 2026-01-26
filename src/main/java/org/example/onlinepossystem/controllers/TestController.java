package org.example.onlinepossystem.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping("/test")
    public String testPage(Model model) {
        model.addAttribute("message", "Thymeleaf is working!");
        return "test"; // will look for test.html
    }
}
