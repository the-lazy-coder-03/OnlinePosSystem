package org.example.onlinepossystem.catalog.menu.dto;

import java.math.BigDecimal;

public record ModifierOptionItem(Integer optionId, String name, Integer menuItemId, BigDecimal price) {
}
