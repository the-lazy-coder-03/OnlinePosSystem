package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface customerRepository extends JpaRepository<Customer, Long> {
}
