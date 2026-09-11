package org.example.onlinepossystem.ordering.api;

import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;

public interface OrderEventPublisher {
    void orderCreated(OrderResponseDTO order, String customerUsername);

    void orderStatusChanged(OrderResponseDTO order, String customerUsername);
}
