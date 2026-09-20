package org.example.onlinepossystem.catalog.pizza.dto;

import java.math.BigDecimal;

public record PizzaBaseOptionItem(Integer pizzaBaseOptionId, String name, BigDecimal extraPrice) {
}
