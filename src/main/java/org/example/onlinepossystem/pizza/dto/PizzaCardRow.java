package org.example.onlinepossystem.pizza.dto;

public record PizzaCardRow(
        Integer pizzaId,
        String name,
        Integer pizzaCategoryId,
        String pizzaCategoryName,
        Integer sizeCm,
        Double basePrice,
        Integer pizzaSortOrder,
        Integer sizeSortOrder
) {
}
