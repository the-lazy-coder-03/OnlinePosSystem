package org.example.onlinepossystem.menu.dto;

public record MenuItemRow(
        Integer menuItemId,
        String name,
        String description,
        Integer categoryId,
        String categoryName,
        Double price
) {
}
