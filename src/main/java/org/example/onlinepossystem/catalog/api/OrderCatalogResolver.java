package org.example.onlinepossystem.catalog.api;

import org.example.onlinepossystem.catalog.dto.MenuDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
            Integer pizzaBaseOptionId,
            List<CatalogCustomizationRequest> customizations
    );

    Optional<NamedReference> findMenuItem(Integer id);

    Optional<NamedReference> findPizza(Integer id);

    Optional<SizeReference> findPizzaSize(Integer id);

    Optional<NamedReference> findIngredient(Integer id);

    Optional<NamedReference> findBurgerComponent(Integer id);

    Optional<NamedReference> findPizzaBaseOption(Integer id);

    record CatalogCustomizationRequest(Integer id, Integer quantity, String type) {
    }

    record ResolvedMenuItem(
            Integer menuItemId,
            String menuItemName,
            Double unitPrice,
            ResolvedBurgerSelection burgerSelection,
            List<ResolvedGenericMenuExtra> extras
    ) {
    }

    record ResolvedPizzaItem(
            Integer pizzaId,
            String pizzaName,
            Integer pizzaSizeId,
            Integer pizzaSizeCm,
            Double basePrice,
            ResolvedPizzaBaseOption baseOption,
            List<ResolvedPizzaExtra> extras,
            List<ResolvedPizzaRemovedIngredient> removedIngredients
    ) {
    }

    record ResolvedPizzaBaseOption(Integer pizzaBaseOptionId, String name, Double unitPrice) {
    }

    record ResolvedPizzaExtra(Integer ingredientId, String ingredientName, Integer quantity, Double unitPrice) {
    }

    record ResolvedPizzaRemovedIngredient(Integer ingredientId, String ingredientName) {
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

    record ResolvedBurgerProtein(Integer componentId, String componentName, Integer quantity, BigDecimal unitPrice) {
    }

    record ResolvedBurgerComponent(Integer componentId, String componentName, Integer quantity, BigDecimal unitPrice) {
    }

    record ResolvedGenericMenuExtra(String name, Integer quantity, Double unitPrice) {
    }

    record NamedReference(Integer id, String name) {
    }

    record SizeReference(Integer id, Integer cm) {
    }
}
