package org.example.onlinepossystem.catalog.menu.dto;

public record MenuItemRow(
        Integer menuItemId,
        String name,
        String description,
        Integer categoryId,
        String categoryName,
        Double price,
        boolean is300ml,
        boolean is2l
) {
}
