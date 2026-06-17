package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.Customer;
import org.example.onlinepossystem.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    void deleteByCustomer(Customer customer);
}
