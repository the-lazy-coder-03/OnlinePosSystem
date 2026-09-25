package org.example.onlinepossystem.special.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record AdminSpecialRequest(
        @NotBlank @Size(max = 80) String code,
        @NotNull Integer branchId,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal bundlePrice,
        boolean active,
        LocalDate startsOn,
        LocalDate endsOn,
        @NotEmpty Set<@Min(1) @Max(7) Integer> days,
        @Min(0) int sortOrder,
        @NotEmpty List<@Valid Component> components,
        List<@Valid Addon> addons
) {
    public record Component(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 255) String label,
            @NotBlank String productType,
            @Min(1) @Max(20) int quantity,
            @NotBlank String selectionMode,
            Integer menuCategoryId,
            Integer pizzaCategoryId,
            Integer pizzaSizeId,
            boolean allowRepeats,
            boolean allowCustomization,
            @Min(0) int sortOrder,
            Set<Integer> menuItemIds,
            Set<Integer> pizzaIds
    ) {}

    public record Addon(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 255) String label,
            @NotBlank String productType,
            Integer menuItemId,
            Integer pizzaId,
            Integer pizzaSizeId,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
            @Min(1) @Max(20) int maxQuantity,
            boolean allowCustomization,
            boolean active,
            @Min(0) int sortOrder
    ) {}
}
