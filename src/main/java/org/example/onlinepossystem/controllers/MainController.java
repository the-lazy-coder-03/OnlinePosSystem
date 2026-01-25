package org.example.onlinepossystem.controllers;

import org.example.onlinepossystem.repository.CustomerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    private final CustomerRepository customerRepository;

    public MainController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping({"/", "/home"})
    public String mainPage(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName(); // email is the principal
            customerRepository.findByEmail(email).ifPresent(customer -> {
                model.addAttribute("customerName", customer.getName());
            });
        }
        return "index"; // return index.html from templates
    }

    @GetMapping("/menu")
    public String menuPage() {
        return "menu"; // menu.html, public
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // login.html
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register"; // register.html
    }
}
