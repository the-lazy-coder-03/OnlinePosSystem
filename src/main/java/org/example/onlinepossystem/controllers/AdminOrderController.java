package org.example.onlinepossystem.controllers;

import jakarta.validation.Valid;
import org.example.onlinepossystem.dto.OrderResponseDTO;
import org.example.onlinepossystem.dto.OrderWebhookRequest;
import org.example.onlinepossystem.service.OrderLiveUpdateService;
import org.example.onlinepossystem.service.OrderService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;
    private final OrderLiveUpdateService orderLiveUpdateService;

    public AdminOrderController(OrderService orderService, OrderLiveUpdateService orderLiveUpdateService) {
        this.orderService = orderService;
        this.orderLiveUpdateService = orderLiveUpdateService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getOrders(
            @RequestParam(value = "branch", required = false) String branch
    ) {
        if (branch == null || branch.isBlank() || "All Branches".equalsIgnoreCase(branch)) {
            return ResponseEntity.ok(orderService.getAllOrders());
        }
        return ResponseEntity.ok(orderService.getOrdersByBranch(branch));
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrders() {
        return orderLiveUpdateService.connectAdminStream();
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> receiveOrderNotification(@Valid @RequestBody OrderWebhookRequest request) {
        OrderResponseDTO order = orderService.getOrderById(request.orderId());
        orderLiveUpdateService.sendOrderToAdmins(order);
        return ResponseEntity.ok(Map.of("success", true, "orderId", order.getId()));
    }
}
