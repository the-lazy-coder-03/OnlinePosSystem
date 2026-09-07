package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class OrderRequestValidator {
    public void validate(OrderRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Order request is required.");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must include at least one item.");
        }
        for (OrderRequestDTO.OrderItemRequestDTO item : request.getItems()) {
            if (item == null) {
                throw new IllegalArgumentException("Order items cannot be null.");
            }
            boolean hasMenuItem = item.getMenuItemId() != null;
            boolean hasPizza = item.getPizzaId() != null;
            if (hasMenuItem == hasPizza) {
                throw new IllegalArgumentException("Each item must include exactly one of menuItemId or pizzaId.");
            }
        }
    }
}
