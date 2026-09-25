package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class OrderRequestValidator {
    private static final String GATE_ACCESS_PATTERN = "[A-Za-z0-9 #*]*";

    public void validate(OrderRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Order request is required.");
        }
        validateGateAccessCode(request.getGateAccessCode());
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

    private void validateGateAccessCode(String value) {
        if (value == null) return;
        if (value.length() > 64) {
            throw new IllegalArgumentException("Gate access code must be 64 characters or fewer.");
        }
        if (!value.matches(GATE_ACCESS_PATTERN)) {
            throw new IllegalArgumentException("Gate access code may contain only letters, numbers, spaces, # and *.");
        }
    }
}
