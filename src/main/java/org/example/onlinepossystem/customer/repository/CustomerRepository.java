package org.example.onlinepossystem.customer.repository;

import org.example.onlinepossystem.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("""
            select c from Customer c
            where :query = '' or lower(concat(coalesce(c.firstName, ''), ' ', coalesce(c.lastName, ''), ' ',
                    coalesce(c.email, ''), ' ', coalesce(c.phone1, ''), ' ', coalesce(c.phone2, '')))
                    like concat('%', :query, '%')
            """)
    Page<Customer> searchForAdmin(@Param("query") String query, Pageable pageable);

    // Search by email
    Optional<Customer> findByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    // Search by phone1
    Optional<Customer> findByPhone1(String phone1);

    // Search by phone2
    Optional<Customer> findByPhone2(String phone2);
}
