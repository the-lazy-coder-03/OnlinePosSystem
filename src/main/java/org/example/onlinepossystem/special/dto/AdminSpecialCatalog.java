package org.example.onlinepossystem.special.dto;

import java.util.List;

public record AdminSpecialCatalog(List<Reference> branches, List<Reference> menuCategories,
                                  List<Reference> pizzaCategories, List<Reference> pizzaSizes,
                                  List<Product> menuItems, List<Product> pizzas) {
    public record Reference(Integer id, String name) {}
    public record Product(Integer id, String name, Integer categoryId) {}
}
