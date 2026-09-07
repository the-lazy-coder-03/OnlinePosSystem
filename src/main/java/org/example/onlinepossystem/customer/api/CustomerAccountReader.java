package org.example.onlinepossystem.customer.api;

import org.example.onlinepossystem.customer.entity.Customer;

import java.util.Optional;

public interface CustomerAccountReader {
    Optional<Customer> findByEmail(String email);
}
