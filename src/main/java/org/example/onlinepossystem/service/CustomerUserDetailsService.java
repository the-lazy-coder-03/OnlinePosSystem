package org.example.onlinepossystem.service;

import org.example.onlinepossystem.entity.Customer;
import org.example.onlinepossystem.repository.CustomerRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomerUserDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrPhone)
            throws UsernameNotFoundException {

        if (usernameOrPhone == null || usernameOrPhone.isBlank()) {
            throw new UsernameNotFoundException("Empty username or phone");
        }

        // Normalize input (VERY important)
        String input = usernameOrPhone.trim();

        Customer customer = customerRepository.findByEmail(input)
                .or(() -> customerRepository.findByPhone1(input))
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        return User.builder()
                .username(customer.getEmail())      // principal
                .password(customer.getPassword())   // already encoded
                .roles("USER")
                .accountLocked(false)
                .disabled(false)
                .build();
    }
}
