package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

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
                    .authorities(authoritiesFor(3))
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
                .authorities(authoritiesFor(effectiveLevel(customer)))
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

    private int effectiveLevel(Customer customer) {
        if (customer.getAccessLevel() != null) {
            return customer.getAccessLevel();
        }
        String role = customer.getRole() == null ? "" : customer.getRole().replace("ROLE_", "").trim().toUpperCase();
        return switch (role) {
            case "ADMIN", "SUPER_ADMIN" -> 3;
            case "DRIVER" -> 4;
            default -> 0;
        };
    }

    private List<SimpleGrantedAuthority> authoritiesFor(int accessLevel) {
        return switch (accessLevel) {
            case 1, 2 -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case 3 -> List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")
            );
            case 4 -> List.of(new SimpleGrantedAuthority("ROLE_DRIVER"));
            default -> List.of(new SimpleGrantedAuthority("ROLE_USER"));
        };
    }
}
