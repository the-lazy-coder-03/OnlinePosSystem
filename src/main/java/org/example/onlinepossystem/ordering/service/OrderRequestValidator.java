package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class OrderRequestValidator {
    public void validate(OrderRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Order request is required.");
        }
        boolean hasItems = request.getItems() != null && !request.getItems().isEmpty();
        boolean hasSpecials = request.getSpecialItems() != null && !request.getSpecialItems().isEmpty();
        if (!hasItems && !hasSpecials) {
            throw new IllegalArgumentException("Order must include at least one item.");
        }
        if (!hasItems) {
            return;
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
