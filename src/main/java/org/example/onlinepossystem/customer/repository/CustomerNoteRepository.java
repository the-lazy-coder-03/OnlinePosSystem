package org.example.onlinepossystem.customer.repository;

import org.example.onlinepossystem.customer.entity.CustomerNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {
    Page<CustomerNote> findByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId, Pageable pageable);
}
