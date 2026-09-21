package org.example.onlinepossystem.ordering.web;

import jakarta.validation.Valid;
import org.example.onlinepossystem.ordering.api.OrderOperations;
import org.example.onlinepossystem.ordering.api.OrderRealtimePublisher;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.dto.OrderWebhookRequest;
import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.customer.api.AccountAccess;
import org.example.onlinepossystem.customer.api.AccountAccessReader;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final AccountAccessReader accountAccessReader;
    private final BranchLookup branchLookup;

    public AdminOrderController(OrderOperations orderOperations,
                                OrderRealtimePublisher orderRealtimePublisher,
                                AccountAccessReader accountAccessReader,
                                BranchLookup branchLookup) {
        this.orderOperations = orderOperations;
        this.orderRealtimePublisher = orderRealtimePublisher;
        this.accountAccessReader = accountAccessReader;
        this.branchLookup = branchLookup;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getOrders(
            @RequestParam(value = "branch", required = false) String branch,
            @RequestParam(value = "branchId", required = false) Integer branchId,
            Authentication authentication
    ) {
        AccountAccess access = access(authentication);
        if (access.branchId() != null) {
            requireBranch(access, access.branchId());
            if (branchId != null && !access.branchId().equals(branchId)) {
                throw new AccessDeniedException("This admin account cannot view the requested branch.");
            }
            if (branch != null && !branch.isBlank() && !"All Branches".equalsIgnoreCase(branch)
                    && !branchLookup.requireById(access.branchId()).name().equalsIgnoreCase(branch)) {
                throw new AccessDeniedException("This admin account cannot view the requested branch.");
            }
            return ResponseEntity.ok(orderOperations.getOrdersByBranch(branchLookup.requireById(access.branchId()).name()));
        }
        if (branchId != null) {
            return ResponseEntity.ok(orderOperations.getOrdersByBranch(branchLookup.requireById(branchId).name()));
        }
        if (branch == null || branch.isBlank() || "All Branches".equalsIgnoreCase(branch)) {
            return ResponseEntity.ok(orderOperations.getAllOrders());
        }
        return ResponseEntity.ok(orderOperations.getOrdersByBranch(branch));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        requireBranch(access(authentication), orderOperations.getOrderById(id).getBranchId());
        return ResponseEntity.ok(orderOperations.updateOrderStatus(id, request.get("status")));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> receiveOrderNotification(@Valid @RequestBody OrderWebhookRequest request,
                                                                         Authentication authentication) {
        OrderResponseDTO order = orderOperations.getOrderById(request.orderId());
        requireBranch(access(authentication), order.getBranchId());
        orderRealtimePublisher.publishToAdmins(order);
        return ResponseEntity.ok(Map.of("success", true, "orderId", order.getId()));
    }

    private AccountAccess access(Authentication authentication) {
        return accountAccessReader.findByUsername(authentication.getName());
    }

    private void requireBranch(AccountAccess access, Integer branchId) {
        if (!access.canAccessBranch(branchId)) {
            throw new AccessDeniedException("This admin account cannot manage the requested branch.");
        }
    }
}
