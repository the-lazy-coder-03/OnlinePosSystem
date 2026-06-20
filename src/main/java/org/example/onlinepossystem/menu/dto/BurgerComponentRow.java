package org.example.onlinepossystem.menu.dto;

public record BurgerComponentRow(
        Integer burgerId,
        Integer componentId,
        String name,
        String componentType,
        Boolean defaultSelected,
        Double price,
        Boolean removable,
        Integer sortOrder,
        Integer proteinQuantityRequired
) {
}
