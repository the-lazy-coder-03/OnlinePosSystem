package org.example.onlinepossystem.ordering.api;

import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;

public interface OrderRealtimePublisher {
    void publishToAdmins(OrderResponseDTO order);
}
