package org.example.onlinepossystem.customer.persistence;

import org.example.onlinepossystem.customer.entity.Customer;

import java.time.LocalDateTime;
import java.util.Optional;

/** Minimal persistence operations needed before an account can authenticate. */
public interface AccountBootstrapStore {
    record Credentials(Long id, String email, String password, int accessLevel) {}
    Optional<Credentials> findCredentials(String identifier);
    boolean emailExists(String email);
    boolean phoneExists(String phone);
    Customer register(Customer customer);
    Optional<String> createReset(String email, String tokenHash, LocalDateTime expiresAt);
    void cancelReset(String tokenHash);
    boolean consumeReset(String tokenHash, String passwordHash);
}
