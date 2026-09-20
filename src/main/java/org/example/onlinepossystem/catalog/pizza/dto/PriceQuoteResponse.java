package org.example.onlinepossystem.catalog.pizza.dto;

import java.math.BigDecimal;

public record PriceQuoteResponse(
        Integer pizzaId,
        Integer sizeCm,
        BigDecimal basePrice,
        BigDecimal toppingsTotal,
        BigDecimal baseOptionTotal,
        BigDecimal total
) {
}
