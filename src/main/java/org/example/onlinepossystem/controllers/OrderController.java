package org.example.onlinepossystem.controllers;

import jakarta.validation.Valid;
import org.example.onlinepossystem.dto.MenuDTO;
import org.example.onlinepossystem.dto.OrderRequestDTO;
import org.example.onlinepossystem.dto.OrderResponseDTO;
import org.example.onlinepossystem.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Orders.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * GET /api/orders/menu?branch=Kenridge
     * Returns all menu items with prices for the selected branch and available toppings/ingredients.
     */
    @GetMapping("/menu")
    public ResponseEntity<List<MenuDTO>> getMenu(@RequestParam String branch) {
        return ResponseEntity.ok(orderService.getMenuForBranch(branch));
    }

    /**
     * POST /api/orders
     * Place a new order.
     */
    @PostMapping
    public ResponseEntity<OrderResponseDTO> placeOrder(@Valid @RequestBody OrderRequestDTO request) {
        return ResponseEntity.ok(orderService.placeOrder(request));
    }

    /**
     * GET /api/orders?branch=Kenridge
     */
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByBranch(@RequestParam String branch) {
        return ResponseEntity.ok(orderService.getOrdersByBranch(branch));
    }

    /**
     * GET /api/orders/pending?branch=Kenridge
     */
    @GetMapping("/pending")
    public ResponseEntity<List<OrderResponseDTO>> getPendingOrdersByBranch(@RequestParam String branch) {
        return ResponseEntity.ok(orderService.getPendingOrdersByBranch(branch));
    }

    /**
     * PUT /api/orders/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String newStatus = request.get("status");

        if (newStatus == null || (!newStatus.equals("Pending")
                && !newStatus.equals("Preparing")
                && !newStatus.equals("Completed")
                && !newStatus.equals("Rejected"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid status. Must be Pending, Preparing, Completed, or Rejected."));
        }

        OrderResponseDTO updatedOrder = orderService.updateOrderStatus(id, newStatus);
        return ResponseEntity.ok(updatedOrder);
    }
}
