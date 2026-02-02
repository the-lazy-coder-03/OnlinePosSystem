package org.example.onlinepossystem.menu.dto;

import java.math.BigDecimal;
import java.util.List;

public record MenuItemDetail(Integer menuItemId, String name, String description, String categoryName, BigDecimal price, List<ModifierGroupItem> modifierGroups) {
    public MenuItemDetail {
        if (modifierGroups == null) {
            modifierGroups = List.of();
        }
    }
}
