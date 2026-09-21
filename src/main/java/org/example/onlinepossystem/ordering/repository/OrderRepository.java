package org.example.onlinepossystem.ordering.repository;

import org.example.onlinepossystem.ordering.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Collection;

@Repository
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBranchIdOrderByCreatedAtDesc(Integer branchId);
    List<Order> findByBranchIdAndStatusOrderByCreatedAtDesc(Integer branchId, String status);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
    List<Order> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end);
    List<Order> findByBranchIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            Integer branchId, LocalDateTime start, LocalDateTime end);
    List<Order> findByStatusInOrderByCreatedAtDesc(Collection<String> statuses);
    List<Order> findByBranchIdAndStatusInOrderByCreatedAtDesc(Integer branchId, Collection<String> statuses);
}
