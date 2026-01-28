package org.example.onlinepossystem.controllers;

import org.example.onlinepossystem.entity.Order;
import org.example.onlinepossystem.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Orders.
 * Handles fetching branch-specific orders and updating order status.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * GET /api/orders?branch=Kenridge
     * Fetch all orders for a specific branch in FIFO order.
     *
     * Query Parameter: branch (required)
     * Response: List of Order objects
     */
    @GetMapping
    public ResponseEntity<List<Order>> getOrdersByBranch(@RequestParam String branch) {
        if (branch == null || branch.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Order> orders = orderService.getOrdersByBranch(branch);
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /api/orders/pending?branch=Kenridge
     * Fetch only pending orders for a specific branch in FIFO order.
     *
     * Query Parameter: branch (required)
     * Response: List of pending Order objects
     */
    @GetMapping("/pending")
    public ResponseEntity<List<Order>> getPendingOrdersByBranch(@RequestParam String branch) {
        if (branch == null || branch.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Order> orders = orderService.getPendingOrdersByBranch(branch);
        return ResponseEntity.ok(orders);
    }

    /**
     * PUT /api/orders/{id}/status
     * Update the status of an order (Accept or Reject).
     *
     * Path Variable: id (order ID)
     * Request Body: { "status": "Accepted" } or { "status": "Rejected" }
     * Response:
     *   - Success: Updated Order object
     *   - Failure: { "success": false, "message": "Order not found" }
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String newStatus = request.get("status");

        if (newStatus == null || (!newStatus.equals("Accepted") && !newStatus.equals("Rejected"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid status. Must be 'Accepted' or 'Rejected'"));
        }

        Order updatedOrder = orderService.updateOrderStatus(id, newStatus);

        if (updatedOrder != null) {
            return ResponseEntity.ok(updatedOrder);
        } else {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Order not found"));
        }
    }

    /**
     * POST /api/orders
     * Create a new order (for testing or external integrations).
     *
     * Request Body: {
     *   "branch": "Kenridge",
     *   "customerName": "Alice",
     *   "type": "Pickup",
     *   "items": "[\"Pizza\",\"Soda\"]"
     * }
     *
     * Response: Created Order object
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Map<String, String> request) {
        String branch = request.get("branch");
        String customerName = request.get("customerName");
        String type = request.get("type");
        String items = request.get("items");

        if (branch == null || customerName == null || type == null || items == null) {
            return ResponseEntity.badRequest().build();
        }

        Order order = orderService.createOrder(branch, customerName, type, items);
        return ResponseEntity.ok(order);
    }
}
