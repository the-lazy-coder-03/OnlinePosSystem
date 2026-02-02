package org.example.onlinepossystem.pizza.dto;

import java.math.BigDecimal;

public record PriceQuoteResponse(Integer pizzaId, Integer sizeCm, BigDecimal basePrice, BigDecimal toppingsTotal, BigDecimal total) {
}
