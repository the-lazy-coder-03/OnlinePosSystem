package org.example.onlinepossystem.controllers;

import org.example.onlinepossystem.entity.Customer;
import org.example.onlinepossystem.repository.CustomerRepository;
import org.example.onlinepossystem.service.CustomerUserDetailsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomerUserDetailsService userDetailsService;

    public CustomerController(CustomerRepository customerRepository,
                              PasswordEncoder passwordEncoder,
                              AuthenticationManager authenticationManager,
                              CustomerUserDetailsService userDetailsService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
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

        // Save customer
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);
        customer.setPassword(passwordEncoder.encode(password)); // encrypt
        customer.setHouseNumber(house_number);
        customer.setStreet(street);
        customer.setArea(area);
        customer.setPostalCode(postalCode);
        customer.setPhone1(phone);
        customer.setPhone2(phone2);
        customer.setPreferredStore(preferred_store);
        customer.setComplexName(complex_name);

        customerRepository.save(customer);

        // Perform actual authentication
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(email, password);

        Authentication auth = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "redirect:/home"; // now logged in
    }
}
