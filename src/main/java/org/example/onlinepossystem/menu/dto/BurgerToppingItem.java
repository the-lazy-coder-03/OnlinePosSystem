package org.example.onlinepossystem.menu.dto;

import java.math.BigDecimal;

public record BurgerToppingItem(
        Integer toppingId,
        String name,
        boolean defaultSelected,
        BigDecimal price,
        String componentType,
        boolean removable,
        Integer proteinQuantityRequired
) {
}
