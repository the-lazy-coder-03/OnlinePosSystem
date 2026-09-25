package org.example.onlinepossystem.catalog.service;

import org.example.onlinepossystem.catalog.api.SpecialCatalogAccess;
import org.example.onlinepossystem.catalog.entity.*;
import org.example.onlinepossystem.catalog.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class SpecialCatalogService implements SpecialCatalogAccess {
    private final MenuItemRepository menuItems;
    private final PizzaRepository pizzas;
    private final MenuCategoryRepository menuCategories;
    private final PizzaCategoryRepository pizzaCategories;
    private final PizzaSizeRepository pizzaSizes;
    private final BranchMenuItemPriceRepository menuPrices;
    private final BranchPizzaPriceRepository pizzaPrices;

    public SpecialCatalogService(MenuItemRepository menuItems, PizzaRepository pizzas,
                                 MenuCategoryRepository menuCategories, PizzaCategoryRepository pizzaCategories,
                                 PizzaSizeRepository pizzaSizes, BranchMenuItemPriceRepository menuPrices,
                                 BranchPizzaPriceRepository pizzaPrices) {
        this.menuItems = menuItems;
        this.pizzas = pizzas;
        this.menuCategories = menuCategories;
        this.pizzaCategories = pizzaCategories;
        this.pizzaSizes = pizzaSizes;
        this.menuPrices = menuPrices;
        this.pizzaPrices = pizzaPrices;
    }

    public Optional<MenuItem> findMenuItem(Integer id) { return menuItems.findById(id); }
    public Optional<Pizza> findPizza(Integer id) { return pizzas.findById(id); }
    public MenuCategory requireMenuCategory(Integer id) { return menuCategories.findById(id).orElseThrow(() -> new IllegalArgumentException("Menu category not found.")); }
    public PizzaCategory requirePizzaCategory(Integer id) { return pizzaCategories.findById(id).orElseThrow(() -> new IllegalArgumentException("Pizza category not found.")); }
    public PizzaSize requirePizzaSize(Integer id) { return pizzaSizes.findById(id).orElseThrow(() -> new IllegalArgumentException("Pizza size not found.")); }
    public Collection<MenuItem> findMenuItems(Set<Integer> ids) { return menuItems.findAllById(ids); }
    public Collection<Pizza> findPizzas(Set<Integer> ids) { return pizzas.findAllById(ids); }
    public List<MenuItem> activeMenuItems() { return menuItems.findAllByActiveTrue(); }
    public List<Pizza> activePizzas() { return pizzas.findAllByActiveTrue(); }
    public Optional<BigDecimal> menuPrice(Integer branchId, Integer menuItemId) {
        return menuPrices.findByBranchIdAndMenuItemId(branchId, menuItemId).map(value -> BigDecimal.valueOf(value.getPrice()));
    }
    public Optional<BigDecimal> pizzaPrice(Integer branchId, Integer pizzaId, Integer pizzaSizeId) {
        return pizzaPrices.findByBranchIdAndPizzaIdAndPizzaSizeId(branchId, pizzaId, pizzaSizeId)
                .map(value -> BigDecimal.valueOf(value.getPrice()));
    }
    public List<Reference> menuCategories() { return menuCategories.findAll().stream().map(value -> new Reference(value.getId(), value.getName(), null)).toList(); }
    public List<Reference> pizzaCategories() { return pizzaCategories.findAll().stream().map(value -> new Reference(value.getId(), value.getName(), null)).toList(); }
    public List<Reference> pizzaSizes() { return pizzaSizes.findAll().stream().map(value -> new Reference(value.getId(), value.getCm() + "cm", null)).toList(); }
    public List<Reference> menuItems() { return menuItems.findAll().stream().map(value -> new Reference(value.getId(), value.getName(), value.getCategory() == null ? null : value.getCategory().getId())).toList(); }
    public List<Reference> pizzas() { return pizzas.findAll().stream().map(value -> new Reference(value.getId(), value.getName(), value.getCategory() == null ? null : value.getCategory().getId())).toList(); }
}
