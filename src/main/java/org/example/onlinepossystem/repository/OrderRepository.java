package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBranchIdOrderByCreatedAtAsc(Integer branchId);

    List<Order> findByBranchIdAndStatusOrderByCreatedAtAsc(Integer branchId, String status);
}
