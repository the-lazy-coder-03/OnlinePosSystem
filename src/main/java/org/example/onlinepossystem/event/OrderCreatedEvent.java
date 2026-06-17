package org.example.onlinepossystem.event;

import org.example.onlinepossystem.dto.OrderResponseDTO;

public record OrderCreatedEvent(OrderResponseDTO order) {
}
