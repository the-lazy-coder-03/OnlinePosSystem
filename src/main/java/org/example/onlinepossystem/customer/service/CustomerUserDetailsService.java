package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;
    private final String adminUsername;
    private final String adminPassword;

    public CustomerUserDetailsService(
            CustomerRepository customerRepository,
            @Value("${ADMIN_USERNAME}") String adminUsername,
            @Value("${ADMIN_PASSWORD}") String adminPassword
    ) {
        this.customerRepository = customerRepository;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {

        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException("Identifier cannot be empty");
        }

        String normalizedIdentifier = identifier.trim();

        // Check if it's the hardcoded admin
        if (normalizedIdentifier.equals(adminUsername)) {
            return User.builder()
                    .username(adminUsername)
                    .password("{noop}" + adminPassword)
                    .roles("ADMIN", "USER")
                    .build();
        }

        Customer customer = customerRepository.findByEmail(normalizedIdentifier)
                .or(() -> customerRepository.findByPhone1(normalizedIdentifier))
                .or(() -> customerRepository.findByPhone2(normalizedIdentifier))
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with identifier: " + normalizedIdentifier));

        return User.builder()
                .username(customer.getEmail())
                .password(formatPasswordForSpringSecurity(customer.getPassword()))
                .roles(safeRole(customer.getRole()))
                .accountLocked(false)
                .disabled(false)
                .build();
    }

    private String formatPasswordForSpringSecurity(String storedPassword) {
        if (storedPassword != null && storedPassword.startsWith("{")) {
            return storedPassword;
        }
        return "{bcrypt}" + storedPassword;
    }

    private String safeRole(String role) {
        if (role == null || role.isBlank()) {
            return "USER";
        }
        return role.replace("ROLE_", "").trim().toUpperCase();
    }
}
