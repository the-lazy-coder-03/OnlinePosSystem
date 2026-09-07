package org.example.onlinepossystem.catalog.menu.dto;

import java.math.BigDecimal;
import java.util.List;

public record MenuItemDetail(
        Integer menuItemId,
        String name,
        String description,
        Integer categoryId,
        String categoryName,
        BigDecimal price,
        List<ModifierGroupItem> modifierGroups,
        List<BurgerToppingItem> burgerToppings
) {
    public MenuItemDetail {
        if (modifierGroups == null) {
            modifierGroups = List.of();
        }
        if (burgerToppings == null) {
            burgerToppings = List.of();
        }
    }
}
