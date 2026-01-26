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
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {

        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException("Identifier cannot be empty");
        }

        String normalizedIdentifier = identifier.trim();

        Customer customer = customerRepository.findByEmail(normalizedIdentifier)
                .or(() -> customerRepository.findByPhone1(normalizedIdentifier))
                .or(() -> customerRepository.findByPhone2(normalizedIdentifier))
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with identifier: " + normalizedIdentifier));

        return User.builder()
                .username(customer.getEmail())
                .password(customer.getPassword()) // must be BCrypt-encoded
                .roles("USER")
                .accountLocked(false)
                .disabled(false)
                .build();
        //
    }
}
