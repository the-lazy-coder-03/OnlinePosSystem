package org.example.onlinepossystem.customer.api;

import java.util.Optional;

public interface CustomerAccountReader {
    Optional<CustomerAccount> findByEmail(String email);

    Optional<CustomerAccount> findById(Long id);
}
