package org.example.onlinepossystem.customer.repository;

import org.example.onlinepossystem.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Search by email
    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByZitadelSubject(String zitadelSubject);

    // Search by phone1
    Optional<Customer> findByPhone1(String phone1);

    // Search by phone2
    Optional<Customer> findByPhone2(String phone2);
}
