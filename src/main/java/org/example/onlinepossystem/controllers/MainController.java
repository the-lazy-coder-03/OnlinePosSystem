package org.example.onlinepossystem.controllers;

import org.example.onlinepossystem.entity.Customer;
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

    // ====== Public Pages ======

    @GetMapping({"/", "/home"})
    public String mainPage(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            customerRepository.findByEmail(email).ifPresent(customer -> {
                model.addAttribute("customerName", customer.getName());
                model.addAttribute("user", customer);
            });
        }
        return "index";
    }

    @GetMapping("/menu")
    public String menuPage() {
        return "menu";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // ====== Private Pages ======

    @GetMapping("/profile/edit")
    public String editProfilePage(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            return customerRepository.findByEmail(email)
                    .map(customer -> {
                        model.addAttribute("customer", customer);
                        return "customerInfoEdit";
                    })
                    .orElse("redirect:/login");
        }
        return "redirect:/login";
    }
}
