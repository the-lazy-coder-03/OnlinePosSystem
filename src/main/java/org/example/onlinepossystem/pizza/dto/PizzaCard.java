package org.example.onlinepossystem.pizza.dto;

import java.math.BigDecimal;

public record PizzaCard(Integer pizzaId, String name, Integer defaultSizeCm, BigDecimal basePrice) {
}
