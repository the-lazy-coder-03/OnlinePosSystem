package org.example.onlinepossystem.pizza.dto;

import java.math.BigDecimal;
import java.util.List;

public record PizzaDetail(
        Integer pizzaId,
        String name,
        List<SizePrice> availableSizes,
        BigDecimal basePriceForSelectedSize,
        List<ToppingItem> defaultToppings,
        List<ToppingItem> allToppings,
        List<ToppingPrice> extraPrices
) {
}
