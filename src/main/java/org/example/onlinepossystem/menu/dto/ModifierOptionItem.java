package org.example.onlinepossystem.menu.dto;

import java.math.BigDecimal;

public record ModifierOptionItem(Integer optionId, String name, Integer menuItemId, BigDecimal price) {
}
