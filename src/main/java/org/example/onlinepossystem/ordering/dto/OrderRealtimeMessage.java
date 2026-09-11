package org.example.onlinepossystem.ordering.dto;

public record OrderRealtimeMessage(
        String eventType,
        Long orderId,
        Integer branchId,
        String branchName,
        String status,
        String message,
        OrderResponseDTO order
) {
}
