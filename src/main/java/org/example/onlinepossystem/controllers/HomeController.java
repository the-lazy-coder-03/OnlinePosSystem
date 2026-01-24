    package org.example.onlinepossystem.controllers;

    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.GetMapping;

    @Controller
    public class HomeController {

        @GetMapping("/")
        public String index() {
            return "index"; // maps to templates/index.html
        }

        @GetMapping("/register")
        public String register() {
            return "register"; // maps to templates/register.html
        }
    }
