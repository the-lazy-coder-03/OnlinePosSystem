package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBranchId(Integer branchId);
    List<Order> findByBranchIdOrderByCreatedAtDesc(Integer branchId);
    List<Order> findByBranchIdAndStatus(Integer branchId, String status);
    List<Order> findByBranchIdAndStatusOrderByCreatedAtDesc(Integer branchId, String status);
    List<Order> findAllByOrderByCreatedAtDesc();
}
