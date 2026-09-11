package org.example.onlinepossystem.ordering.event;

import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;

public record OrderCreatedEvent(OrderResponseDTO order, String customerUsername) {
}
