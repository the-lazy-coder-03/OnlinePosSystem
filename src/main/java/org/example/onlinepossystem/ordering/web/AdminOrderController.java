package org.example.onlinepossystem.ordering.web;

import jakarta.validation.Valid;
import org.example.onlinepossystem.ordering.api.OrderOperations;
import org.example.onlinepossystem.ordering.api.OrderRealtimePublisher;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.dto.OrderWebhookRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderOperations orderOperations;
    private final OrderRealtimePublisher orderRealtimePublisher;

    public AdminOrderController(OrderOperations orderOperations, OrderRealtimePublisher orderRealtimePublisher) {
        this.orderOperations = orderOperations;
        this.orderRealtimePublisher = orderRealtimePublisher;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getOrders(
            @RequestParam(value = "branch", required = false) String branch
    ) {
        if (branch == null || branch.isBlank() || "All Branches".equalsIgnoreCase(branch)) {
            return ResponseEntity.ok(orderOperations.getAllOrders());
        }
        return ResponseEntity.ok(orderOperations.getOrdersByBranch(branch));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {
        return ResponseEntity.ok(orderOperations.updateOrderStatus(id, request.get("status")));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> receiveOrderNotification(@Valid @RequestBody OrderWebhookRequest request) {
        OrderResponseDTO order = orderOperations.getOrderById(request.orderId());
        orderRealtimePublisher.publishToAdmins(order);
        return ResponseEntity.ok(Map.of("success", true, "orderId", order.getId()));
    }
}
