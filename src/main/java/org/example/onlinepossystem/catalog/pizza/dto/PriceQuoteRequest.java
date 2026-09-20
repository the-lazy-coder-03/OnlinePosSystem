package org.example.onlinepossystem.catalog.pizza.dto;

import java.util.List;

public record PriceQuoteRequest(
        Integer pizzaId,
        Integer branchId,
        Integer sizeCm,
        List<Integer> selectedToppingIds,
        Integer pizzaBaseOptionId
) {
    public PriceQuoteRequest {
        if (selectedToppingIds == null) {
            selectedToppingIds = List.of();
        }
    }
}
