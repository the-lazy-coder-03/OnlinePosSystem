package org.example.onlinepossystem.catalog.api;

import org.example.onlinepossystem.catalog.dto.MenuDTO;
import org.example.onlinepossystem.catalog.entity.BurgerComponent;
import org.example.onlinepossystem.catalog.entity.Ingredient;
import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.entity.Pizza;
import org.example.onlinepossystem.catalog.entity.PizzaSize;

import java.math.BigDecimal;
import java.util.List;

public interface OrderCatalogResolver {
    List<MenuDTO> getMenuForBranch(String branchName);

    ResolvedMenuItem resolveMenuItem(
            Integer branchId,
            Integer menuItemId,
            List<CatalogCustomizationRequest> customizations
    );

    ResolvedPizzaItem resolvePizzaItem(
            Integer branchId,
            Integer pizzaId,
            Integer pizzaSizeId,
            Integer sizeCm,
            List<CatalogCustomizationRequest> customizations
    );

    record CatalogCustomizationRequest(Integer id, Integer quantity, String type) {
    }

    record ResolvedMenuItem(
            MenuItem menuItem,
            Double unitPrice,
            ResolvedBurgerSelection burgerSelection,
            List<ResolvedGenericMenuExtra> extras
    ) {
    }

    record ResolvedPizzaItem(
            Pizza pizza,
            PizzaSize pizzaSize,
            Double basePrice,
            List<ResolvedPizzaExtra> extras
    ) {
    }

    record ResolvedPizzaExtra(Ingredient ingredient, Integer quantity, Double unitPrice) {
    }

    record ResolvedBurgerSelection(
            ResolvedBurgerProtein protein,
            List<ResolvedBurgerComponent> removedComponents,
            List<ResolvedBurgerComponent> extraComponents
    ) {
        public static ResolvedBurgerSelection empty() {
            return new ResolvedBurgerSelection(null, List.of(), List.of());
        }
    }

    record ResolvedBurgerProtein(BurgerComponent component, Integer quantity, BigDecimal unitPrice) {
    }

    record ResolvedBurgerComponent(BurgerComponent component, Integer quantity, BigDecimal unitPrice) {
    }

    record ResolvedGenericMenuExtra(String name, Integer quantity, Double unitPrice) {
    }
}
