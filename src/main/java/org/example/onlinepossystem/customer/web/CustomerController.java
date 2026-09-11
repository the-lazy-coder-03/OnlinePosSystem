package org.example.onlinepossystem.customer.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.example.onlinepossystem.customer.dto.ProfilePageView;
import org.example.onlinepossystem.customer.service.CustomerProfileService;
import org.example.onlinepossystem.customer.service.CustomerRegistrationException;
import org.example.onlinepossystem.customer.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Validated
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;
    private final CustomerProfileService customerProfileService;

    public CustomerController(CustomerService customerService,
                              CustomerProfileService customerProfileService) {
        this.customerService = customerService;
        this.customerProfileService = customerProfileService;
    }

    @GetMapping("/profile/edit")
    public String editProfilePage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        return customerProfileService.getProfilePage(authentication.getName())
                .map(profile -> populateProfileModel(model, profile))
                .orElse("redirect:/login");
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam @NotBlank String firstName,
            @RequestParam @NotBlank String lastName,
            @RequestParam(required = false) String houseNumber,
            @RequestParam(required = false) String street,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$") String phone1,
            @RequestParam(required = false) String phone2,
            @RequestParam(required = false) String preferredStore,
            @RequestParam(required = false) String complexName,
            @RequestParam(required = false, name = "password") String newPassword,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            customerService.updateProfile(
                    authentication.getName(),
                    firstName,
                    lastName,
                    houseNumber,
                    street,
                    area,
                    postalCode,
                    phone1,
                    phone2,
                    preferredStore,
                    complexName,
                    newPassword
            );
        } catch (IllegalArgumentException ex) {
            return "redirect:/profile/edit?error=password";
        }

        return "redirect:/profile/edit?success";
    }

    @PostMapping("/register")
    public String handleRegister(
            @RequestParam @NotBlank String firstName,
            @RequestParam @NotBlank String lastName,
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String password,
            @RequestParam(required = false, name = "house_number") String houseNumber,
            @RequestParam @NotBlank String street,
            @RequestParam @NotBlank String area,
            @RequestParam @NotBlank String postalCode,
            @RequestParam @NotBlank @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$") String phone,
            @RequestParam(required = false) String phone2,
            @RequestParam(required = false, name = "preferred_store") String preferredStore
    ) {
        try {
            customerService.registerCustomer(
                    firstName,
                    lastName,
                    email,
                    password,
                    phone,
                    phone2,
                    houseNumber,
                    street,
                    area,
                    null,
                    preferredStore,
                    postalCode
            );
        } catch (CustomerRegistrationException ex) {
            return switch (ex.getReason()) {
                case PASSWORD -> "redirect:/register?error=password";
                case EMAIL -> "redirect:/register?error=email";
                case PHONE -> "redirect:/register?error=phone";
            };
        }
        logger.info("New customer account registered");

        return "redirect:/login?registered";
    }

    private String populateProfileModel(Model model, ProfilePageView profile) {
        model.addAttribute("customer", profile.customer());
        model.addAttribute("recentOrders", profile.recentOrders());
        return "customerInfoEdit";
    }
}
