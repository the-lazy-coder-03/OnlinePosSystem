package org.example.onlinepossystem.special.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record SpecialView(Long id, String code, String name, String description, BigDecimal price, Integer branchId,
                          boolean active, boolean archived, LocalDate startsOn, LocalDate endsOn,
                          Set<Integer> days, int sortOrder, List<Component> components, List<Addon> addons) {
    public record Component(Long id, String code, String label, String productType, int quantity, String selectionMode,
                            Integer menuCategoryId, Integer pizzaCategoryId, Integer pizzaSizeId, Integer sizeCm,
                            boolean allowRepeats, boolean allowCustomization, int sortOrder,
                            List<Option> options) {}
    public record Option(Integer id, String name, String categoryName) {}
    public record Addon(Long id, String code, String label, String productType, Integer productId, String productName,
                        Integer pizzaSizeId,
                        Integer sizeCm, BigDecimal price, int maxQuantity, boolean allowCustomization,
                        boolean active, int sortOrder) {}
}
