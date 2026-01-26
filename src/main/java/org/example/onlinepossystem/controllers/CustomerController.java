package org.example.onlinepossystem.controllers;

import org.example.onlinepossystem.entity.Customer;
import org.example.onlinepossystem.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerController(CustomerRepository customerRepository,
                              PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public String handleRegister(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String house_number,
            @RequestParam String street,
            @RequestParam String area,
            @RequestParam String postalCode,
            @RequestParam String phone,
            @RequestParam(required = false) String phone2,
            @RequestParam String preferred_store,
            @RequestParam(required = false) String complex_name
    ) {
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
        customer.setName(name);
        customer.setEmail(email);
        customer.setPassword(passwordEncoder.encode(password));
        customer.setHouseNumber(house_number);
        customer.setStreet(street);
        customer.setArea(area);
        customer.setPostalCode(postalCode);
        customer.setPhone1(phone);
        customer.setPhone2(phone2);
        customer.setPreferredStore(preferred_store);
        customer.setComplexName(complex_name);

        customerRepository.save(customer);

        // Redirect to login page after registration
        return "redirect:/login?registered";
    }
}
