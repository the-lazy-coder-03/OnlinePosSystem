package org.example.onlinepossystem.catalog.pizza.dto;

import java.math.BigDecimal;

public record PizzaCard(
        Integer pizzaId,
        String name,
        Integer pizzaCategoryId,
        String pizzaCategoryName,
        Integer defaultSizeCm,
        BigDecimal basePrice
) {
}
