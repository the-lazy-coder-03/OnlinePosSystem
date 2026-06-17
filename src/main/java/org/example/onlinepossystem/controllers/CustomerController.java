package org.example.onlinepossystem.controllers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.example.onlinepossystem.entity.Customer;
import org.example.onlinepossystem.repository.CustomerRepository;
import org.example.onlinepossystem.security.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;

@Controller
@Validated
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerController(CustomerRepository customerRepository,
                              PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
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

        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new java.util.NoSuchElementException("Customer not found for email: " + email));

        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setHouseNumber(houseNumber);
        customer.setStreet(street);
        customer.setArea(area);
        customer.setPostalCode(postalCode);
        customer.setPhone1(phone1);
        customer.setPhone2(phone2);
        customer.setPreferredStore(preferredStore);
        customer.setComplexName(complexName);

        if (newPassword != null && !newPassword.isEmpty()) {
            if (!PasswordPolicy.isValid(newPassword)) {
                return "redirect:/profile/edit?error=password";
            }
            customer.setPassword(passwordEncoder.encode(newPassword));
        }

        customerRepository.save(customer);

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
        if (!PasswordPolicy.isValid(password)) {
            return "redirect:/register?error=password";
        }

        // Check if email already exists
        if (customerRepository.findByEmail(email).isPresent()) {
            return "redirect:/register?error=email";
        }

        // Check if phone number already exists
        if (customerRepository.findByPhone1(phone).isPresent()) {
            return "redirect:/register?error=phone";
        }

        // Save customer with encoded password
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setPassword(passwordEncoder.encode(password));
        customer.setHouseNumber(houseNumber);
        customer.setStreet(street);
        customer.setArea(area);
        customer.setPostalCode(postalCode);
        customer.setPhone1(phone);
        customer.setPhone2(phone2);
        customer.setPreferredStore(preferredStore);
        customer.setRole("USER");

        customerRepository.save(customer);
        logger.info("New customer account registered");

        // Redirect to login page after registration
        return "redirect:/login?registered";
    }
}
