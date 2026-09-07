package org.example.onlinepossystem.catalog.menu.dto;

import java.math.BigDecimal;

public record ModifierOptionRow(Integer groupId, Integer optionId, String name, Integer menuItemId, BigDecimal price) {
}
