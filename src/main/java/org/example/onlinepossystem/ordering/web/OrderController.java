package org.example.onlinepossystem.ordering.web;

import jakarta.validation.Valid;
import org.example.onlinepossystem.catalog.api.OrderCatalogResolver;
import org.example.onlinepossystem.catalog.dto.MenuDTO;
import org.example.onlinepossystem.ordering.api.OrderOperations;
import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Orders.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderOperations orderOperations;
    private final OrderCatalogResolver catalogResolver;

    public OrderController(OrderOperations orderOperations, OrderCatalogResolver catalogResolver) {
        this.orderOperations = orderOperations;
        this.catalogResolver = catalogResolver;
    }

    /**
     * GET /api/orders/menu?branch=Kenridge
     * Returns all menu items with prices for the selected branch and available toppings/ingredients.
     */
    @GetMapping("/menu")
    public ResponseEntity<List<MenuDTO>> getMenu(@RequestParam String branch) {
        return ResponseEntity.ok(catalogResolver.getMenuForBranch(branch));
    }

    /**
     * POST /api/orders
     * Place a new order.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponseDTO> placeOrder(
            @Valid @RequestBody OrderRequestDTO request,
            Authentication authentication
    ) {
        String customerEmail = authentication == null ? null : authentication.getName();
        return ResponseEntity.ok(orderOperations.placeOrderForCustomer(request, customerEmail));
    }

    /**
     * GET /api/orders?branch=Kenridge
     */
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByBranch(@RequestParam String branch) {
        return ResponseEntity.ok(orderOperations.getOrdersByBranch(branch));
    }

    /**
     * GET /api/orders/pending?branch=Kenridge
     */
    @GetMapping("/pending")
    public ResponseEntity<List<OrderResponseDTO>> getPendingOrdersByBranch(@RequestParam String branch) {
        return ResponseEntity.ok(orderOperations.getPendingOrdersByBranch(branch));
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

        OrderResponseDTO updatedOrder = orderOperations.updateOrderStatus(id, newStatus);
        return ResponseEntity.ok(updatedOrder);
    }
}
