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
    public UserDetails loadUserByUsername(String usernameOrPhone) throws UsernameNotFoundException {
        System.out.println("=== Attempting to load user: " + usernameOrPhone);

        // Try email first, then phone1
        Customer customer = customerRepository.findByEmail(usernameOrPhone)
                .or(() -> customerRepository.findByPhone1(usernameOrPhone))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrPhone));

        System.out.println("=== User found: " + customer.getEmail());
        System.out.println("=== Password hash from DB: " + customer.getPassword().substring(0, 20) + "...");

        return User.builder()
                .username(customer.getEmail()) // Spring uses email as principal
                .password(customer.getPassword()) // must be hashed
                .roles("USER")
                .build();
    }
}
