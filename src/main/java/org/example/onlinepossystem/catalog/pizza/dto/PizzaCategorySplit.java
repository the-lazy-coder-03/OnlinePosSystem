package org.example.onlinepossystem.catalog.pizza.dto;

import java.util.List;

public record PizzaCategorySplit(List<PizzaCard> favouritePizzas, List<PizzaCard> supremePizzas) {
    public PizzaCategorySplit {
        if (favouritePizzas == null) {
            favouritePizzas = List.of();
        }
        if (supremePizzas == null) {
            supremePizzas = List.of();
        }
    }
}
