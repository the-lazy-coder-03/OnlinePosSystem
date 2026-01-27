package org.example.onlinepossystem.service;

import org.example.onlinepossystem.entity.Order;
import org.example.onlinepossystem.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Get all orders for a specific branch in FIFO order (oldest first).
     * Includes pending, accepted, and rejected orders.
     */
    public List<Order> getOrdersByBranch(String branch) {
        return orderRepository.findByBranchOrderByCreatedAtAsc(branch);
    }

    /**
     * Get only pending orders for a specific branch in FIFO order.
     */
    public List<Order> getPendingOrdersByBranch(String branch) {
        return orderRepository.findByBranchAndStatusOrderByCreatedAtAsc(branch, "Pending");
    }

    /**
     * Update the status of an order (Accept or Reject).
     *
     * @param orderId Order ID to update
     * @param newStatus New status ("Accepted" or "Rejected")
     * @return Updated Order, or null if not found
     */
    public Order updateOrderStatus(Long orderId, String newStatus) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(newStatus);
            return orderRepository.save(order);
        }
        return null;
    }

    /**
     * Create a new order (for testing or external integrations).
     */
    public Order createOrder(String branch, String customerName, String type, String items) {
        Order order = new Order(branch, customerName, type, items);
        return orderRepository.save(order);
    }

    /**
     * Get all orders (for admin purposes).
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
