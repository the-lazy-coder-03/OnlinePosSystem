package org.example.onlinepossystem.catalog.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CatalogAdministration {
    Map<String, Object> getDashboardAttributes(Integer branchId);

    void savePizza(Integer id, String name, Integer categoryId, String description,
                   List<Integer> ingredientIds, Map<String, String> parameters, String actor);

    void deletePizza(Integer id, String actor);

    void updatePizzaPrice(Integer branchId, Integer pizzaId, Integer pizzaSizeId, Double price, String actor);

    void saveMenuItem(Integer id, String name, Integer categoryId, String description,
                      List<Integer> modifierGroupIds, Map<String, String> parameters, String actor);

    void deleteMenuItem(Integer id, String actor);

    void updateMenuItemPrice(Integer branchId, Integer menuItemId, Double price, String actor);

    void updateToppingPrice(Integer branchId, Integer priceCategoryId, Integer pizzaSizeId, Double price, String actor);

    void savePizzaCategory(Integer id, String name, Map<String, String> parameters, String actor);

    void saveMenuCategory(Integer id, String name, Map<String, String> parameters, String actor);

    void savePizzaSize(Integer id, Integer cm, Map<String, String> parameters, String actor);

    void savePriceCategory(Integer id, String name, Map<String, String> parameters, String actor);

    void saveIngredient(Integer id, String name, Integer priceCategoryId, Map<String, String> parameters, String actor);

    void saveModifierGroup(Integer id, String name, Integer minSelect, Integer maxSelect,
                           Map<String, String> parameters, String actor);

    void saveModifierOption(Integer id, Integer groupId, String name, String menuItemId,
                            BigDecimal additionalPrice, String actor);
}
