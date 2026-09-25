package org.example.onlinepossystem.catalog.api;

import org.example.onlinepossystem.catalog.entity.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SpecialCatalogAccess {
    record Reference(Integer id, String name, Integer categoryId) {}

    Optional<MenuItem> findMenuItem(Integer id);
    Optional<Pizza> findPizza(Integer id);
    MenuCategory requireMenuCategory(Integer id);
    PizzaCategory requirePizzaCategory(Integer id);
    PizzaSize requirePizzaSize(Integer id);
    Collection<MenuItem> findMenuItems(Set<Integer> ids);
    Collection<Pizza> findPizzas(Set<Integer> ids);
    List<MenuItem> activeMenuItems();
    List<Pizza> activePizzas();
    Optional<BigDecimal> menuPrice(Integer branchId, Integer menuItemId);
    Optional<BigDecimal> pizzaPrice(Integer branchId, Integer pizzaId, Integer pizzaSizeId);
    List<Reference> menuCategories();
    List<Reference> pizzaCategories();
    List<Reference> pizzaSizes();
    List<Reference> menuItems();
    List<Reference> pizzas();
}
