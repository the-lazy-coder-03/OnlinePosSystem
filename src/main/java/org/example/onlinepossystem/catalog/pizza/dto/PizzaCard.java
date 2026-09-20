package org.example.onlinepossystem.catalog.pizza.dto;

import java.math.BigDecimal;
import java.util.List;

public record PizzaCard(
        Integer pizzaId,
        String name,
        Integer pizzaCategoryId,
        String pizzaCategoryName,
        Integer defaultSizeCm,
        BigDecimal basePrice,
        List<String> defaultToppings
) {
    public PizzaCard {
        if (defaultToppings == null) {
            defaultToppings = List.of();
        }
    }
}
