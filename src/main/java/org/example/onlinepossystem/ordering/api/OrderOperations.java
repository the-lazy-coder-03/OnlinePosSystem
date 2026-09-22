package org.example.onlinepossystem.ordering.api;

import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;

import java.util.List;

public interface OrderOperations {
    OrderResponseDTO placeOrder(OrderRequestDTO request);

    OrderResponseDTO placeOrderForCustomer(OrderRequestDTO request, String customerEmail);
    OrderResponseDTO placeOrderForEnvironmentAdmin(OrderRequestDTO request);

    List<OrderResponseDTO> getOrdersByBranch(String branchName);

    List<OrderResponseDTO> getPendingOrdersByBranch(String branchName);

    OrderResponseDTO updateOrderStatus(Long orderId, String newStatus);

    List<OrderResponseDTO> getAllOrders();

    OrderResponseDTO getOrderById(Long orderId);
}
