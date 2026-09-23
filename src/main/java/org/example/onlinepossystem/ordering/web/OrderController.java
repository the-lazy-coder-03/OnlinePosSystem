package org.example.onlinepossystem.ordering.web;

import jakarta.validation.Valid;
import org.example.onlinepossystem.catalog.api.OrderCatalogResolver;
import org.example.onlinepossystem.catalog.dto.MenuDTO;
import org.example.onlinepossystem.customer.api.EnvironmentAdminAccount;
import org.example.onlinepossystem.ordering.api.OrderOperations;
import org.example.onlinepossystem.ordering.api.InvalidOrderStatusException;
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
    private final EnvironmentAdminAccount environmentAdminAccount;

    public OrderController(OrderOperations orderOperations, OrderCatalogResolver catalogResolver,
                           EnvironmentAdminAccount environmentAdminAccount) {
        this.orderOperations = orderOperations;
        this.catalogResolver = catalogResolver;
        this.environmentAdminAccount = environmentAdminAccount;
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
        if (authentication != null && environmentAdminAccount.matches(authentication.getPrincipal())) {
            Long customerId = environmentAdminAccount.ensureCustomerId(authentication.getPrincipal());
            return ResponseEntity.ok(orderOperations.placeOrderForCustomerId(request, customerId));
        }
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

        try {
            return ResponseEntity.ok(orderOperations.updateOrderStatus(id, request.get("status")));
        } catch (InvalidOrderStatusException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }
}
