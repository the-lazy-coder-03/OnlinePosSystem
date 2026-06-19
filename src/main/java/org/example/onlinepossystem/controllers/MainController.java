package org.example.onlinepossystem.controllers;

import org.example.onlinepossystem.entity.Customer;
import org.example.onlinepossystem.repository.CustomerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

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
                model.addAttribute("customerName", customer.getFirstName());
                model.addAttribute("user", customer);
            });
        }
        return "index";
    }

    @GetMapping("/menu")
    public String menuPage(Model model) {
        model.addAttribute("menuOnly", true);
        return "PlaceOrder";
    }

    @GetMapping("/menu/kenridge")
    public String kenridgeMenu(Model model) {
        model.addAttribute("menuOnly", true);
        model.addAttribute("branchId", 1);
        model.addAttribute("branchName", "Kenridge");
        return "PlaceOrder";
    }

    @GetMapping("/menu/uitzicht")
    public String uitzichtMenu(Model model) {
        model.addAttribute("menuOnly", true);
        model.addAttribute("branchId", 2);
        model.addAttribute("branchName", "Uitzicht");
        return "PlaceOrder";
    }

    @GetMapping("/order")
    public String orderPage(
            @RequestParam(value = "branchId", required = false) Integer branchId,
            Model model,
            Authentication authentication
    ) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            customerRepository.findByEmail(email).ifPresent(customer -> {
                model.addAttribute("customerName", customer.getFirstName());
                model.addAttribute("user", customer);
            });
        }
        
        // Default to branch 1 if nothing is provided
        model.addAttribute("branchId", branchId != null ? branchId : 1);
        
        return "PlaceOrder";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/admin/login")
    public String adminLoginPage() {
        return "admin-login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping({"/input-orders", "/orders", "/InputOrders", "/InputOrders.html"})
    public String inputOrdersPage() {
        return "InputOrders";
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
