package org.example.onlinepossystem.shared.web;

import org.example.onlinepossystem.customer.api.CustomerAccountReader;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MainController {

    private final CustomerAccountReader customerAccountReader;

    public MainController(CustomerAccountReader customerAccountReader) {
        this.customerAccountReader = customerAccountReader;
    }

    // ====== Public Pages ======

    @GetMapping({"/", "/home"})
    public String mainPage(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            customerAccountReader.findByEmail(email).ifPresent(customer -> {
                model.addAttribute("customerName", customer.firstName());
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
            customerAccountReader.findByEmail(email).ifPresent(customer -> {
                model.addAttribute("customerName", customer.firstName());
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
}
