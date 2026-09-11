package org.example.onlinepossystem.ordering.event;

import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;

public record OrderStatusChangedEvent(OrderResponseDTO order, String customerUsername) {
}
