package org.example.onlinepossystem.customer.repository;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);

    void deleteByCustomer(Customer customer);

    void deleteByCustomerAndUsedFalse(Customer customer);
}
