package org.example.onlinepossystem.catalog.pizza.dto;

import java.util.List;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PriceQuoteRequest(
        @Positive Integer pizzaId,
        @Positive Integer branchId,
        @Positive Integer sizeCm,
        @Size(max = 100) List<@Positive Integer> selectedToppingIds,
        @Positive Integer pizzaBaseOptionId
) {
    public PriceQuoteRequest {
        if (selectedToppingIds == null) {
            selectedToppingIds = List.of();
        }
    }
}
