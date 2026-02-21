package org.example.onlinepossystem.pizza.dto;

import java.util.List;

public record PriceQuoteRequest(Integer pizzaId, Integer branchId, Integer sizeCm, List<Integer> selectedToppingIds) {
    public PriceQuoteRequest {
        if (selectedToppingIds == null) {
            selectedToppingIds = List.of();
        }
    }
}
