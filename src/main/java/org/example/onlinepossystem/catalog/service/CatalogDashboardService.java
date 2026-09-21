package org.example.onlinepossystem.catalog.service;

import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.branch.api.BranchView;
import org.example.onlinepossystem.catalog.entity.BranchMenuItemPrice;
import org.example.onlinepossystem.catalog.entity.BranchPizzaPrice;
import org.example.onlinepossystem.catalog.entity.Ingredient;
import org.example.onlinepossystem.catalog.entity.MenuCategory;
import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.entity.MenuItemModifierGroup;
import org.example.onlinepossystem.catalog.entity.ModifierGroup;
import org.example.onlinepossystem.catalog.entity.ModifierOption;
import org.example.onlinepossystem.catalog.entity.Pizza;
import org.example.onlinepossystem.catalog.entity.PizzaCategory;
import org.example.onlinepossystem.catalog.entity.PizzaDefaultIngredient;
import org.example.onlinepossystem.catalog.entity.PizzaSize;
import org.example.onlinepossystem.catalog.entity.PriceCategory;
import org.example.onlinepossystem.catalog.repository.BranchExtraPriceRepository;
import org.example.onlinepossystem.catalog.repository.BranchMenuItemPriceRepository;
import org.example.onlinepossystem.catalog.repository.BranchPizzaPriceRepository;
import org.example.onlinepossystem.catalog.repository.IngredientRepository;
import org.example.onlinepossystem.catalog.repository.MenuCategoryRepository;
import org.example.onlinepossystem.catalog.repository.MenuItemModifierGroupRepository;
import org.example.onlinepossystem.catalog.repository.MenuItemRepository;
import org.example.onlinepossystem.catalog.repository.ModifierGroupRepository;
import org.example.onlinepossystem.catalog.repository.ModifierOptionRepository;
import org.example.onlinepossystem.catalog.repository.PizzaCategoryRepository;
import org.example.onlinepossystem.catalog.repository.PizzaDefaultIngredientRepository;
import org.example.onlinepossystem.catalog.repository.PizzaRepository;
import org.example.onlinepossystem.catalog.repository.PizzaSizeRepository;
import org.example.onlinepossystem.catalog.repository.PriceCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CatalogDashboardService {
    private final PizzaRepository pizzaRepository;
    private final PizzaCategoryRepository pizzaCategoryRepository;
    private final PizzaSizeRepository pizzaSizeRepository;
    private final BranchLookup branchLookup;
    private final BranchPizzaPriceRepository branchPizzaPriceRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final BranchMenuItemPriceRepository branchMenuItemPriceRepository;
    private final PriceCategoryRepository priceCategoryRepository;
    private final BranchExtraPriceRepository branchExtraPriceRepository;
    private final IngredientRepository ingredientRepository;
    private final PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository;
    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierOptionRepository modifierOptionRepository;
    private final MenuItemModifierGroupRepository menuItemModifierGroupRepository;

    public CatalogDashboardService(
            PizzaRepository pizzaRepository,
            PizzaCategoryRepository pizzaCategoryRepository,
            PizzaSizeRepository pizzaSizeRepository,
            BranchLookup branchLookup,
            BranchPizzaPriceRepository branchPizzaPriceRepository,
            MenuItemRepository menuItemRepository,
            MenuCategoryRepository menuCategoryRepository,
            BranchMenuItemPriceRepository branchMenuItemPriceRepository,
            PriceCategoryRepository priceCategoryRepository,
            BranchExtraPriceRepository branchExtraPriceRepository,
            IngredientRepository ingredientRepository,
            PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository,
            ModifierGroupRepository modifierGroupRepository,
            ModifierOptionRepository modifierOptionRepository,
            MenuItemModifierGroupRepository menuItemModifierGroupRepository
    ) {
        this.pizzaRepository = pizzaRepository;
        this.pizzaCategoryRepository = pizzaCategoryRepository;
        this.pizzaSizeRepository = pizzaSizeRepository;
        this.branchLookup = branchLookup;
        this.branchPizzaPriceRepository = branchPizzaPriceRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.branchMenuItemPriceRepository = branchMenuItemPriceRepository;
        this.priceCategoryRepository = priceCategoryRepository;
        this.branchExtraPriceRepository = branchExtraPriceRepository;
        this.ingredientRepository = ingredientRepository;
        this.pizzaDefaultIngredientRepository = pizzaDefaultIngredientRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierOptionRepository = modifierOptionRepository;
        this.menuItemModifierGroupRepository = menuItemModifierGroupRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardAttributes(Integer branchId) {
        List<Pizza> pizzas = sorted(pizzaRepository.findAll(), Comparator
                .comparingInt((Pizza p) -> p.getCategory() == null ? 9999 : safeInt(p.getCategory().getSortOrder()))
                .thenComparingInt(p -> safeInt(p.getSortOrder()))
                .thenComparing(Pizza::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        List<MenuItem> menuItems = sorted(menuItemRepository.findAll(), Comparator
                .comparingInt((MenuItem m) -> m.getCategory() == null ? 9999 : safeInt(m.getCategory().getSortOrder()))
                .thenComparingInt(m -> safeInt(m.getSortOrder()))
                .thenComparing(MenuItem::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        List<PizzaCategory> pizzaCategories = sorted(pizzaCategoryRepository.findAll(), Comparator
                .comparingInt((PizzaCategory c) -> safeInt(c.getSortOrder()))
                .thenComparing(PizzaCategory::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        List<MenuCategory> menuCategories = sorted(menuCategoryRepository.findAll(), Comparator
                .comparingInt((MenuCategory c) -> safeInt(c.getSortOrder()))
                .thenComparing(MenuCategory::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        List<PizzaSize> pizzaSizes = sorted(pizzaSizeRepository.findAll(), Comparator
                .comparingInt((PizzaSize s) -> safeInt(s.getSortOrder()))
                .thenComparingInt(s -> safeInt(s.getCm())));
        List<PriceCategory> priceCategories = sorted(priceCategoryRepository.findAll(), Comparator
                .comparingInt((PriceCategory c) -> safeInt(c.getSortOrder()))
                .thenComparing(PriceCategory::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        List<Ingredient> ingredients = sorted(ingredientRepository.findAll(), Comparator
                .comparing((Ingredient i) -> i.getPriceCategory() == null ? "" : safeString(i.getPriceCategory().getName()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Ingredient::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        List<ModifierGroup> modifierGroups = sorted(modifierGroupRepository.findAll(), Comparator
                .comparing(ModifierGroup::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        List<ModifierOption> modifierOptions = sorted(modifierOptionRepository.findAll(), Comparator
                .comparingInt((ModifierOption o) -> safeInt(o.getGroupId()))
                .thenComparing(ModifierOption::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        List<BranchView> branches = sorted(branchLookup.findAll(), Comparator.comparing(BranchView::name));
        if (branchId != null) {
            branches = branches.stream().filter(branch -> branch.id().equals(branchId)).toList();
        }
        Map<Integer, String> branchNames = branches.stream().collect(Collectors.toMap(BranchView::id, BranchView::name));
        List<BranchPizzaPrice> pizzaPrices = branchPizzaPriceRepository.findAll().stream()
                .filter(price -> branchId == null || branchId.equals(price.getBranchId()))
                .toList();
        List<BranchMenuItemPrice> menuPrices = branchMenuItemPriceRepository.findAll().stream()
                .filter(price -> branchId == null || branchId.equals(price.getBranchId()))
                .toList();
        List<PizzaDefaultIngredient> defaultIngredients = pizzaDefaultIngredientRepository.findAll();
        List<MenuItemModifierGroup> menuModifierGroups = menuItemModifierGroupRepository.findAll();

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("pizzas", pizzas);
        attributes.put("menuItems", menuItems);
        attributes.put("branches", branches);
        attributes.put("pizzaCategories", pizzaCategories);
        attributes.put("menuCategories", menuCategories);
        attributes.put("pizzaSizes", pizzaSizes);
        attributes.put("branchPizzaPrices", pizzaPrices.stream()
                .map(price -> new BranchPizzaPriceView(price.getBranchId(), branchNames.get(price.getBranchId()),
                        price.getPizza(), price.getPizzaSize(), price.getPrice()))
                .toList());
        attributes.put("branchMenuItemPrices", menuPrices.stream()
                .map(price -> new BranchMenuItemPriceView(price.getBranchId(), branchNames.get(price.getBranchId()),
                        price.getMenuItem(), price.getPrice()))
                .toList());
        attributes.put("priceCategories", priceCategories);
        attributes.put("branchExtraPrices", branchExtraPriceRepository.findAll().stream()
                .filter(price -> branchId == null || branchId.equals(price.getBranchId()))
                .map(price -> new BranchExtraPriceView(price.getBranchId(), branchNames.get(price.getBranchId()),
                        price.getPriceCategory(), price.getPizzaSize(), price.getPrice()))
                .toList());
        attributes.put("ingredients", ingredients);
        attributes.put("pizzaDefaultIngredients", defaultIngredients);
        attributes.put("modifierGroups", modifierGroups);
        attributes.put("modifierOptions", modifierOptions);
        attributes.put("menuItemModifierGroups", menuModifierGroups);
        attributes.put("pizzaIngredientIdsByPizzaId", defaultIngredients.stream()
                .filter(link -> link.getPizza() != null && link.getIngredient() != null)
                .collect(Collectors.groupingBy(link -> link.getPizza().getId(),
                        Collectors.mapping(link -> String.valueOf(link.getIngredient().getId()), Collectors.joining(",")))));
        attributes.put("modifierGroupIdsByMenuItemId", menuModifierGroups.stream()
                .collect(Collectors.groupingBy(MenuItemModifierGroup::getMenuItemId,
                        Collectors.mapping(link -> String.valueOf(link.getGroupId()), Collectors.joining(",")))));
        attributes.put("activePizzaCount", pizzas.stream().filter(Pizza::isActive).count());
        attributes.put("activeMenuItemCount", menuItems.stream().filter(MenuItem::isActive).count());
        attributes.put("activeIngredientCount", ingredients.stream().filter(Ingredient::isActive).count());
        return attributes;
    }

    private <T> List<T> sorted(List<T> items, Comparator<T> comparator) {
        return items.stream().sorted(comparator).toList();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private record BranchPizzaPriceView(
            Integer branchId, String branchName, Pizza pizza, PizzaSize pizzaSize, Double price
    ) {
    }

    private record BranchMenuItemPriceView(
            Integer branchId, String branchName, MenuItem menuItem, Double price
    ) {
    }

    private record BranchExtraPriceView(
            Integer branchId, String branchName, PriceCategory priceCategory, PizzaSize pizzaSize, Double price
    ) {
    }
}
