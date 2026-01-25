package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Search by email
    Optional<Customer> findByEmail(String email);

    // Search by phone1
    Optional<Customer> findByPhone1(String phone1);
}
