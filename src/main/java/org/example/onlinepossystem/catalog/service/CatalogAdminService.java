package org.example.onlinepossystem.catalog.service;

import org.example.onlinepossystem.catalog.api.CatalogAdministration;
import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.branch.entity.Branch;
import org.example.onlinepossystem.catalog.entity.BranchExtraPrice;
import org.example.onlinepossystem.catalog.entity.BranchMenuItemPrice;
import org.example.onlinepossystem.catalog.entity.BranchPizzaPrice;
import org.example.onlinepossystem.catalog.entity.Ingredient;
import org.example.onlinepossystem.catalog.entity.MenuCategory;
import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.entity.MenuItemModifierGroup;
import org.example.onlinepossystem.catalog.entity.ModifierGroup;
import org.example.onlinepossystem.catalog.entity.ModifierOption;
import org.example.onlinepossystem.catalog.entity.Pizza;
import org.example.onlinepossystem.catalog.entity.PizzaAllowedSize;
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
import org.example.onlinepossystem.catalog.repository.PizzaAllowedSizeRepository;
import org.example.onlinepossystem.catalog.repository.PizzaCategoryRepository;
import org.example.onlinepossystem.catalog.repository.PizzaDefaultIngredientRepository;
import org.example.onlinepossystem.catalog.repository.PizzaRepository;
import org.example.onlinepossystem.catalog.repository.PizzaSizeRepository;
import org.example.onlinepossystem.catalog.repository.PriceCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CatalogAdminService implements CatalogAdministration {

    private static final Logger logger = LoggerFactory.getLogger(CatalogAdminService.class);

    private final PizzaRepository pizzaRepository;
    private final PizzaCategoryRepository pizzaCategoryRepository;
    private final PizzaSizeRepository pizzaSizeRepository;
    private final BranchLookup branchLookup;
    private final BranchPizzaPriceRepository branchPizzaPriceRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final BranchMenuItemPriceRepository branchMenuItemPriceRepository;
    private final PizzaAllowedSizeRepository pizzaAllowedSizeRepository;
    private final PriceCategoryRepository priceCategoryRepository;
    private final BranchExtraPriceRepository branchExtraPriceRepository;
    private final IngredientRepository ingredientRepository;
    private final PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository;
    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierOptionRepository modifierOptionRepository;
    private final MenuItemModifierGroupRepository menuItemModifierGroupRepository;

    public CatalogAdminService(PizzaRepository pizzaRepository,
                           PizzaCategoryRepository pizzaCategoryRepository,
                           PizzaSizeRepository pizzaSizeRepository,
                           BranchLookup branchLookup,
                           BranchPizzaPriceRepository branchPizzaPriceRepository,
                           MenuItemRepository menuItemRepository,
                           MenuCategoryRepository menuCategoryRepository,
                           BranchMenuItemPriceRepository branchMenuItemPriceRepository,
                           PizzaAllowedSizeRepository pizzaAllowedSizeRepository,
                           PriceCategoryRepository priceCategoryRepository,
                           BranchExtraPriceRepository branchExtraPriceRepository,
                           IngredientRepository ingredientRepository,
                           PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository,
                           ModifierGroupRepository modifierGroupRepository,
                           ModifierOptionRepository modifierOptionRepository,
                           MenuItemModifierGroupRepository menuItemModifierGroupRepository) {
        this.pizzaRepository = pizzaRepository;
        this.pizzaCategoryRepository = pizzaCategoryRepository;
        this.pizzaSizeRepository = pizzaSizeRepository;
        this.branchLookup = branchLookup;
        this.branchPizzaPriceRepository = branchPizzaPriceRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.branchMenuItemPriceRepository = branchMenuItemPriceRepository;
        this.pizzaAllowedSizeRepository = pizzaAllowedSizeRepository;
        this.priceCategoryRepository = priceCategoryRepository;
        this.branchExtraPriceRepository = branchExtraPriceRepository;
        this.ingredientRepository = ingredientRepository;
        this.pizzaDefaultIngredientRepository = pizzaDefaultIngredientRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierOptionRepository = modifierOptionRepository;
        this.menuItemModifierGroupRepository = menuItemModifierGroupRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardAttributes() {
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

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("pizzas", pizzas);
        attributes.put("menuItems", menuItems);
        attributes.put("branches", sorted(branchLookup.findAll(), Comparator.comparing(Branch::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))));
        attributes.put("pizzaCategories", pizzaCategories);
        attributes.put("menuCategories", menuCategories);
        attributes.put("pizzaSizes", pizzaSizes);
        List<BranchPizzaPrice> branchPizzaPrices = branchPizzaPriceRepository.findAll();
        List<BranchMenuItemPrice> branchMenuItemPrices = branchMenuItemPriceRepository.findAll();
        List<PizzaDefaultIngredient> pizzaDefaultIngredients = pizzaDefaultIngredientRepository.findAll();
        List<MenuItemModifierGroup> menuItemModifierGroups = menuItemModifierGroupRepository.findAll();

        attributes.put("branchPizzaPrices", branchPizzaPrices);
        attributes.put("branchMenuItemPrices", branchMenuItemPrices);
        attributes.put("priceCategories", priceCategories);
        attributes.put("branchExtraPrices", branchExtraPriceRepository.findAll());
        attributes.put("ingredients", ingredients);
        attributes.put("pizzaDefaultIngredients", pizzaDefaultIngredients);
        attributes.put("modifierGroups", modifierGroups);
        attributes.put("modifierOptions", modifierOptions);
        attributes.put("menuItemModifierGroups", menuItemModifierGroups);
        attributes.put("pizzaIngredientIdsByPizzaId", pizzaDefaultIngredients.stream()
                .filter(pdi -> pdi.getPizza() != null && pdi.getIngredient() != null)
                .collect(Collectors.groupingBy(
                        pdi -> pdi.getPizza().getId(),
                        Collectors.mapping(pdi -> String.valueOf(pdi.getIngredient().getId()), Collectors.joining(",")))));
        attributes.put("modifierGroupIdsByMenuItemId", menuItemModifierGroups.stream()
                .collect(Collectors.groupingBy(
                        MenuItemModifierGroup::getMenuItemId,
                        Collectors.mapping(link -> String.valueOf(link.getGroupId()), Collectors.joining(",")))));
        attributes.put("activePizzaCount", pizzas.stream().filter(Pizza::isActive).count());
        attributes.put("activeMenuItemCount", menuItems.stream().filter(MenuItem::isActive).count());
        attributes.put("activeIngredientCount", ingredients.stream().filter(Ingredient::isActive).count());
        return attributes;
    }

    @Transactional
    @Override
    public void savePizza(Integer id,
                            String name,
                            Integer categoryId,
                            String description,
                            Integer sortOrder,
                            List<Integer> ingredientIds,
                            Map<String, String> params,
                            String actor) {
        Pizza pizza = id == null
                ? new Pizza()
                : pizzaRepository.findById(id).orElseGet(Pizza::new);
        if (pizza.getId() == null) {
            pizza.setId(nextId(pizzaRepository.findAll(), Pizza::getId));
        }

        PizzaCategory category = pizzaCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Pizza category not found with ID: " + categoryId));
        pizza.setCategory(category);
        pizza.setName(cleanText(name));
        pizza.setDescription(cleanText(description));
        pizza.setSortOrder(sortOrder == null ? 0 : sortOrder);
        pizza.setActive(params.containsKey("active"));

        Pizza savedPizza = pizzaRepository.save(pizza);
        replaceDefaultIngredients(savedPizza, ingredientIds);
        savePizzaPriceMatrix(savedPizza, params);

        logger.info("Admin action={} pizzaId={} admin={}", "savePizza", savedPizza.getId(), actorName(actor));
    }

    @Override
    public void deletePizza(Integer id, String actor) {
        pizzaRepository.deleteById(id);
        logger.info("Admin action={} pizzaId={} admin={}", "deletePizza", id, actorName(actor));
    }

    @Override
    public void updatePizzaPrice(Integer branchId,
                                   Integer pizzaId,
                                   Integer pizzaSizeId,
                                   Double price,
                                   String actor) {
        Branch branch = branchLookup.requireById(branchId);
        Pizza pizza = pizzaRepository.findById(pizzaId).orElseThrow(() -> new java.util.NoSuchElementException("Pizza not found with ID: " + pizzaId));
        PizzaSize pizzaSize = pizzaSizeRepository.findById(pizzaSizeId).orElseThrow(() -> new java.util.NoSuchElementException("Pizza size not found with ID: " + pizzaSizeId));
        savePizzaPrice(branch, pizza, pizzaSize, price);

        logger.info("Admin action={} branchId={} pizzaId={} sizeId={} admin={}",
                "updatePizzaPrice", branchId, pizzaId, pizzaSizeId, actorName(actor));
    }

    @Transactional
    @Override
    public void saveMenuItem(Integer id,
                               String name,
                               Integer categoryId,
                               String description,
                               Integer sortOrder,
                               List<Integer> modifierGroupIds,
                               Map<String, String> params,
                               String actor) {
        MenuItem menuItem = id == null
                ? new MenuItem()
                : menuItemRepository.findById(id).orElseGet(MenuItem::new);
        if (menuItem.getId() == null) {
            menuItem.setId(nextId(menuItemRepository.findAll(), MenuItem::getId));
        }

        MenuCategory category = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Menu category not found with ID: " + categoryId));
        menuItem.setCategory(category);
        menuItem.setName(cleanText(name));
        menuItem.setDescription(cleanText(description));
        menuItem.setSortOrder(sortOrder == null ? 0 : sortOrder);
        menuItem.setActive(params.containsKey("active"));
        menuItem.setIs300ml(params.containsKey("is300ml"));
        menuItem.setIs2l(params.containsKey("is2l"));

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        replaceMenuItemModifierGroups(savedMenuItem, modifierGroupIds);
        saveMenuItemPriceMatrix(savedMenuItem, params);

        logger.info("Admin action={} menuItemId={} admin={}", "saveMenuItem", savedMenuItem.getId(), actorName(actor));
    }

    @Override
    public void deleteMenuItem(Integer id, String actor) {
        menuItemRepository.deleteById(id);
        logger.info("Admin action={} menuItemId={} admin={}", "deleteMenuItem", id, actorName(actor));
    }

    @Override
    public void updateMenuItemPrice(Integer branchId,
                                      Integer menuItemId,
                                      Double price,
                                      String actor) {
        Branch branch = branchLookup.requireById(branchId);
        MenuItem menuItem = menuItemRepository.findById(menuItemId).orElseThrow(() -> new java.util.NoSuchElementException("Menu item not found with ID: " + menuItemId));

        saveMenuItemPrice(branch, menuItem, price);
        logger.info("Admin action={} branchId={} menuItemId={} admin={}",
                "updateMenuItemPrice", branchId, menuItemId, actorName(actor));
    }

    @Override
    public void updateToppingPrice(Integer branchId,
                                     Integer priceCategoryId,
                                     Integer pizzaSizeId,
                                     Double price,
                                     String actor) {
        Branch branch = branchLookup.requireById(branchId);
        PriceCategory priceCategory = priceCategoryRepository.findById(priceCategoryId).orElseThrow(() -> new java.util.NoSuchElementException("Price category not found with ID: " + priceCategoryId));
        PizzaSize pizzaSize = pizzaSizeRepository.findById(pizzaSizeId).orElseThrow(() -> new java.util.NoSuchElementException("Pizza size not found with ID: " + pizzaSizeId));

        BranchExtraPrice bep = branchExtraPriceRepository.findById(new BranchExtraPrice.BranchExtraPriceId(branchId, priceCategoryId, pizzaSizeId))
                .orElse(new BranchExtraPrice(branch, priceCategory, pizzaSize, price));
        bep.setPrice(price);
        branchExtraPriceRepository.save(bep);
        logger.info("Admin action={} branchId={} priceCategoryId={} sizeId={} admin={}",
                "updateToppingPrice", branchId, priceCategoryId, pizzaSizeId, actorName(actor));
    }

    @Override
    public void savePizzaCategory(Integer id,
                                    String name,
                                    Integer sortOrder,
                                    Map<String, String> params,
                                    String actor) {
        PizzaCategory category = id == null
                ? new PizzaCategory()
                : pizzaCategoryRepository.findById(id).orElseGet(PizzaCategory::new);
        if (category.getId() == null) {
            category.setId(nextId(pizzaCategoryRepository.findAll(), PizzaCategory::getId));
        }
        category.setName(cleanText(name));
        category.setSortOrder(sortOrder == null ? 0 : sortOrder);
        category.setActive(params.containsKey("active"));
        pizzaCategoryRepository.save(category);
        logger.info("Admin action={} pizzaCategoryId={} admin={}", "savePizzaCategory", category.getId(), actorName(actor));
    }

    @Override
    public void saveMenuCategory(Integer id,
                                   String name,
                                   Integer sortOrder,
                                   Map<String, String> params,
                                   String actor) {
        MenuCategory category = id == null
                ? new MenuCategory()
                : menuCategoryRepository.findById(id).orElseGet(MenuCategory::new);
        if (category.getId() == null) {
            category.setId(nextId(menuCategoryRepository.findAll(), MenuCategory::getId));
        }
        category.setName(cleanText(name));
        category.setSortOrder(sortOrder == null ? 0 : sortOrder);
        category.setActive(params.containsKey("active"));
        menuCategoryRepository.save(category);
        logger.info("Admin action={} menuCategoryId={} admin={}", "saveMenuCategory", category.getId(), actorName(actor));
    }

    @Override
    public void savePizzaSize(Integer id,
                                Integer cm,
                                Integer sortOrder,
                                Map<String, String> params,
                                String actor) {
        PizzaSize size = id == null
                ? new PizzaSize()
                : pizzaSizeRepository.findById(id).orElseGet(PizzaSize::new);
        if (size.getId() == null) {
            size.setId(nextId(pizzaSizeRepository.findAll(), PizzaSize::getId));
        }
        size.setCm(cm);
        size.setSortOrder(sortOrder == null ? 0 : sortOrder);
        size.setActive(params.containsKey("active"));
        pizzaSizeRepository.save(size);
        logger.info("Admin action={} pizzaSizeId={} admin={}", "savePizzaSize", size.getId(), actorName(actor));
    }

    @Override
    public void savePriceCategory(Integer id,
                                    String name,
                                    Integer sortOrder,
                                    Map<String, String> params,
                                    String actor) {
        PriceCategory category = id == null
                ? new PriceCategory()
                : priceCategoryRepository.findById(id).orElseGet(PriceCategory::new);
        if (category.getId() == null) {
            category.setId(nextId(priceCategoryRepository.findAll(), PriceCategory::getId));
        }
        category.setName(cleanText(name));
        category.setSortOrder(sortOrder == null ? 0 : sortOrder);
        category.setActive(params.containsKey("active"));
        priceCategoryRepository.save(category);
        logger.info("Admin action={} priceCategoryId={} admin={}", "savePriceCategory", category.getId(), actorName(actor));
    }

    @Override
    public void saveIngredient(Integer id,
                                 String name,
                                 Integer priceCategoryId,
                                 Map<String, String> params,
                                 String actor) {
        Ingredient ingredient = id == null
                ? new Ingredient()
                : ingredientRepository.findById(id).orElseGet(Ingredient::new);
        if (ingredient.getId() == null) {
            ingredient.setId(nextId(ingredientRepository.findAll(), Ingredient::getId));
        }
        PriceCategory priceCategory = priceCategoryRepository.findById(priceCategoryId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Price category not found with ID: " + priceCategoryId));
        ingredient.setName(cleanText(name));
        ingredient.setPriceCategory(priceCategory);
        ingredient.setActive(params.containsKey("active"));
        ingredient.setSeasonal(params.containsKey("seasonal"));
        ingredientRepository.save(ingredient);
        logger.info("Admin action={} ingredientId={} admin={}", "saveIngredient", ingredient.getId(), actorName(actor));
    }

    @Override
    public void saveModifierGroup(Integer id,
                                    String name,
                                    Integer minSelect,
                                    Integer maxSelect,
                                    Map<String, String> params,
                                    String actor) {
        ModifierGroup group = id == null
                ? new ModifierGroup()
                : modifierGroupRepository.findById(id).orElseGet(ModifierGroup::new);
        if (group.getId() == null) {
            group.setId(nextId(modifierGroupRepository.findAll(), ModifierGroup::getId));
        }
        boolean required = params.containsKey("required");
        int min = minSelect == null ? (required ? 1 : 0) : Math.max(0, minSelect);
        int max = maxSelect == null ? Math.max(1, min) : Math.max(min, maxSelect);
        group.setName(cleanText(name));
        group.setRequired(required);
        group.setMinSelect(min);
        group.setMaxSelect(max);
        modifierGroupRepository.save(group);
        logger.info("Admin action={} modifierGroupId={} admin={}", "saveModifierGroup", group.getId(), actorName(actor));
    }

    @Override
    public void saveModifierOption(Integer id,
                                     Integer groupId,
                                     String name,
                                     String menuItemId,
                                     BigDecimal additionalPrice,
                                     String actor) {
        ModifierOption option = id == null
                ? new ModifierOption()
                : modifierOptionRepository.findById(id).orElseGet(ModifierOption::new);
        if (option.getId() == null) {
            option.setId(nextId(modifierOptionRepository.findAll(), ModifierOption::getId));
        }
        ModifierGroup group = modifierGroupRepository.findById(groupId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Modifier group not found with ID: " + groupId));
        option.setGroupId(group.getId());
        option.setName(cleanText(name));
        option.setMenuItemId(parseInteger(menuItemId).orElse(null));
        option.setAdditionalPrice(additionalPrice);
        modifierOptionRepository.save(option);
        logger.info("Admin action={} modifierOptionId={} admin={}", "saveModifierOption", option.getId(), actorName(actor));
    }

    private void replaceDefaultIngredients(Pizza pizza, List<Integer> ingredientIds) {
        List<PizzaDefaultIngredient> existing = pizzaDefaultIngredientRepository.findByPizzaId(pizza.getId());
        pizzaDefaultIngredientRepository.deleteAll(existing);

        if (ingredientIds == null) {
            return;
        }

        for (int i = 0; i < ingredientIds.size(); i++) {
            Integer ingredientId = ingredientIds.get(i);
            Ingredient ingredient = ingredientRepository.findById(ingredientId).orElse(null);
            if (ingredient != null) {
                pizzaDefaultIngredientRepository.save(new PizzaDefaultIngredient(pizza, ingredient, true, 1, i));
            }
        }
    }

    private void replaceMenuItemModifierGroups(MenuItem menuItem, List<Integer> modifierGroupIds) {
        List<MenuItemModifierGroup> existing = menuItemModifierGroupRepository.findByMenuItemId(menuItem.getId());
        menuItemModifierGroupRepository.deleteAll(existing);

        if (modifierGroupIds == null) {
            return;
        }

        for (Integer groupId : modifierGroupIds) {
            if (modifierGroupRepository.existsById(groupId)) {
                menuItemModifierGroupRepository.save(new MenuItemModifierGroup(menuItem.getId(), groupId));
            }
        }
    }

    private void savePizzaPriceMatrix(Pizza pizza, Map<String, String> params) {
        for (Branch branch : branchLookup.findAll()) {
            for (PizzaSize size : pizzaSizeRepository.findAll()) {
                String key = "pizzaPrice_" + branch.getId() + "_" + size.getId();
                parsePrice(params.get(key)).ifPresent(price -> savePizzaPrice(branch, pizza, size, price));
            }
        }
    }

    private void savePizzaPrice(Branch branch, Pizza pizza, PizzaSize pizzaSize, Double price) {
        BranchPizzaPrice branchPizzaPrice = branchPizzaPriceRepository
                .findByBranchIdAndPizzaIdAndPizzaSizeId(branch.getId(), pizza.getId(), pizzaSize.getId())
                .orElse(new BranchPizzaPrice(branch, pizza, pizzaSize, price));
        branchPizzaPrice.setPrice(price);
        branchPizzaPriceRepository.save(branchPizzaPrice);

        PizzaAllowedSize.PizzaAllowedSizeId allowedSizeId = new PizzaAllowedSize.PizzaAllowedSizeId(pizza.getId(), pizzaSize.getId());
        if (!pizzaAllowedSizeRepository.existsById(allowedSizeId)) {
            pizzaAllowedSizeRepository.save(new PizzaAllowedSize(pizza, pizzaSize));
        }
    }

    private void saveMenuItemPriceMatrix(MenuItem menuItem, Map<String, String> params) {
        for (Branch branch : branchLookup.findAll()) {
            String key = "menuPrice_" + branch.getId();
            parsePrice(params.get(key)).ifPresent(price -> saveMenuItemPrice(branch, menuItem, price));
        }
    }

    private void saveMenuItemPrice(Branch branch, MenuItem menuItem, Double price) {
        BranchMenuItemPrice branchMenuItemPrice = branchMenuItemPriceRepository
                .findByBranchIdAndMenuItemId(branch.getId(), menuItem.getId())
                .orElse(new BranchMenuItemPrice(branch, menuItem, price));
        branchMenuItemPrice.setPrice(price);
        branchMenuItemPriceRepository.save(branchMenuItemPrice);
    }

    private <T> List<T> sorted(List<T> items, Comparator<T> comparator) {
        return items.stream().sorted(comparator).toList();
    }

    private <T> Integer nextId(List<T> items, Function<T, Integer> idExtractor) {
        return items.stream()
                .map(idExtractor)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    private Optional<Double> parsePrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        Double price = Double.valueOf(raw);
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        return Optional.of(price);
    }

    private Optional<Integer> parseInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Integer.valueOf(raw));
    }

    private Integer safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private String cleanText(String value) {
        return value == null ? null : value.trim();
    }

    private String actorName(String actor) {
        return actor == null || actor.isBlank() ? "unknown" : actor;
    }
}
