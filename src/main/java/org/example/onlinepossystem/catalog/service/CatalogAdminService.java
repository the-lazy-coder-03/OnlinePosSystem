package org.example.onlinepossystem.catalog.service;

import org.example.onlinepossystem.catalog.api.CatalogAdministration;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class CatalogAdminService implements CatalogAdministration {
    private final CatalogDashboardService dashboardService;
    private final PizzaCatalogAdminService pizzaAdminService;
    private final MenuCatalogAdminService menuAdminService;
    private final CatalogTaxonomyAdminService taxonomyAdminService;
    private final ModifierCatalogAdminService modifierAdminService;
    private final ToppingPriceAdminService toppingPriceAdminService;

    public CatalogAdminService(
            CatalogDashboardService dashboardService,
            PizzaCatalogAdminService pizzaAdminService,
            MenuCatalogAdminService menuAdminService,
            CatalogTaxonomyAdminService taxonomyAdminService,
            ModifierCatalogAdminService modifierAdminService,
            ToppingPriceAdminService toppingPriceAdminService
    ) {
        this.dashboardService = dashboardService;
        this.pizzaAdminService = pizzaAdminService;
        this.menuAdminService = menuAdminService;
        this.taxonomyAdminService = taxonomyAdminService;
        this.modifierAdminService = modifierAdminService;
        this.toppingPriceAdminService = toppingPriceAdminService;
    }

    @Override
    public Map<String, Object> getDashboardAttributes(Integer branchId) {
        return dashboardService.getDashboardAttributes(branchId);
    }

    @Override
    public void savePizza(Integer id, String name, Integer categoryId, String description, Integer sortOrder,
                          List<Integer> ingredientIds, Map<String, String> parameters, String actor) {
        pizzaAdminService.savePizza(id, name, categoryId, description, sortOrder, ingredientIds, parameters, actor);
    }

    @Override
    public void deletePizza(Integer id, String actor) {
        pizzaAdminService.deletePizza(id, actor);
    }

    @Override
    public void updatePizzaPrice(Integer branchId, Integer pizzaId, Integer pizzaSizeId, Double price, String actor) {
        pizzaAdminService.updatePizzaPrice(branchId, pizzaId, pizzaSizeId, price, actor);
    }

    @Override
    public void saveMenuItem(Integer id, String name, Integer categoryId, String description, Integer sortOrder,
                             List<Integer> modifierGroupIds, Map<String, String> parameters, String actor) {
        menuAdminService.saveMenuItem(id, name, categoryId, description, sortOrder, modifierGroupIds, parameters, actor);
    }

    @Override
    public void deleteMenuItem(Integer id, String actor) {
        menuAdminService.deleteMenuItem(id, actor);
    }

    @Override
    public void updateMenuItemPrice(Integer branchId, Integer menuItemId, Double price, String actor) {
        menuAdminService.updateMenuItemPrice(branchId, menuItemId, price, actor);
    }

    @Override
    public void updateToppingPrice(Integer branchId, Integer priceCategoryId,
                                   Integer pizzaSizeId, Double price, String actor) {
        toppingPriceAdminService.updateToppingPrice(branchId, priceCategoryId, pizzaSizeId, price, actor);
    }

    @Override
    public void savePizzaCategory(Integer id, String name, Integer sortOrder,
                                  Map<String, String> parameters, String actor) {
        taxonomyAdminService.savePizzaCategory(id, name, sortOrder, parameters, actor);
    }

    @Override
    public void saveMenuCategory(Integer id, String name, Integer sortOrder,
                                 Map<String, String> parameters, String actor) {
        taxonomyAdminService.saveMenuCategory(id, name, sortOrder, parameters, actor);
    }

    @Override
    public void savePizzaSize(Integer id, Integer cm, Integer sortOrder,
                              Map<String, String> parameters, String actor) {
        taxonomyAdminService.savePizzaSize(id, cm, sortOrder, parameters, actor);
    }

    @Override
    public void savePriceCategory(Integer id, String name, Integer sortOrder,
                                  Map<String, String> parameters, String actor) {
        taxonomyAdminService.savePriceCategory(id, name, sortOrder, parameters, actor);
    }

    @Override
    public void saveIngredient(Integer id, String name, Integer priceCategoryId,
                               Map<String, String> parameters, String actor) {
        taxonomyAdminService.saveIngredient(id, name, priceCategoryId, parameters, actor);
    }

    @Override
    public void saveModifierGroup(Integer id, String name, Integer minSelect, Integer maxSelect,
                                  Map<String, String> parameters, String actor) {
        modifierAdminService.saveModifierGroup(id, name, minSelect, maxSelect, parameters, actor);
    }

    @Override
    public void saveModifierOption(Integer id, Integer groupId, String name, String menuItemId,
                                   BigDecimal additionalPrice, String actor) {
        modifierAdminService.saveModifierOption(id, groupId, name, menuItemId, additionalPrice, actor);
    }
}
