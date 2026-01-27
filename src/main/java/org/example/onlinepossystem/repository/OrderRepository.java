package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find all orders for a specific branch, ordered by creation time (FIFO).
     * This ensures staff see orders in the order they were placed.
     */
    List<Order> findByBranchOrderByCreatedAtAsc(String branch);

    /**
     * Find all pending orders for a specific branch, ordered by creation time.
     * Useful for fetching only unprocessed orders.
     */
    List<Order> findByBranchAndStatusOrderByCreatedAtAsc(String branch, String status);
}
