package org.example.onlinepossystem.catalog.service;

import org.example.onlinepossystem.branch.entity.Branch;
import org.example.onlinepossystem.branch.repository.BranchRepository;
import org.example.onlinepossystem.catalog.entity.BranchMenuItemPrice;
import org.example.onlinepossystem.catalog.entity.MenuCategory;
import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.entity.MenuItemModifierGroup;
import org.example.onlinepossystem.catalog.entity.ModifierGroup;
import org.example.onlinepossystem.catalog.entity.Pizza;
import org.example.onlinepossystem.catalog.entity.PizzaAllowedSize;
import org.example.onlinepossystem.catalog.entity.PizzaCategory;
import org.example.onlinepossystem.catalog.entity.PizzaDefaultIngredient;
import org.example.onlinepossystem.catalog.entity.PizzaSize;
import org.example.onlinepossystem.catalog.entity.PriceCategory;
import org.example.onlinepossystem.catalog.entity.Ingredient;
import org.example.onlinepossystem.catalog.repository.BranchMenuItemPriceRepository;
import org.example.onlinepossystem.catalog.repository.BranchPizzaPriceRepository;
import org.example.onlinepossystem.catalog.repository.IngredientRepository;
import org.example.onlinepossystem.catalog.repository.MenuCategoryRepository;
import org.example.onlinepossystem.catalog.repository.MenuItemModifierGroupRepository;
import org.example.onlinepossystem.catalog.repository.MenuItemRepository;
import org.example.onlinepossystem.catalog.repository.ModifierGroupRepository;
import org.example.onlinepossystem.catalog.repository.PizzaAllowedSizeRepository;
import org.example.onlinepossystem.catalog.repository.PizzaCategoryRepository;
import org.example.onlinepossystem.catalog.repository.PizzaDefaultIngredientRepository;
import org.example.onlinepossystem.catalog.repository.PizzaRepository;
import org.example.onlinepossystem.catalog.repository.PizzaSizeRepository;
import org.example.onlinepossystem.catalog.repository.PriceCategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CatalogAdminServiceTest {
    @Autowired
    private CatalogAdminService catalogAdminService;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private BranchMenuItemPriceRepository branchMenuItemPriceRepository;

    @Autowired
    private MenuItemModifierGroupRepository menuItemModifierGroupRepository;

    @Autowired
    private ModifierGroupRepository modifierGroupRepository;

    @Autowired
    private PizzaCategoryRepository pizzaCategoryRepository;

    @Autowired
    private PizzaRepository pizzaRepository;

    @Autowired
    private PizzaSizeRepository pizzaSizeRepository;

    @Autowired
    private PriceCategoryRepository priceCategoryRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository;

    @Autowired
    private BranchPizzaPriceRepository branchPizzaPriceRepository;

    @Autowired
    private PizzaAllowedSizeRepository pizzaAllowedSizeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void saveMenuCategoryCreatesAndUpdatesCategory() {
        String suffix = suffix();
        int expectedSortOrder = menuCategoryRepository.findAll().stream()
                .mapToInt(category -> category.getSortOrder() == null ? 0 : category.getSortOrder())
                .max().orElse(0) + 1;

        catalogAdminService.saveMenuCategory(
                null,
                "  Admin Menu Category " + suffix + "  ",
                Map.of("active", "on"),
                null
        );
        MenuCategory created = findMenuCategory("Admin Menu Category " + suffix);

        catalogAdminService.saveMenuCategory(
                created.getId(),
                "Updated Admin Menu Category " + suffix,
                Map.of(),
                null
        );
        MenuCategory updated = menuCategoryRepository.findById(created.getId()).orElseThrow();

        assertThat(updated.getName()).isEqualTo("Updated Admin Menu Category " + suffix);
        assertThat(updated.getSortOrder()).isEqualTo(expectedSortOrder);
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void saveMenuItemPersistsModifierGroupsAndBranchPrices() {
        String suffix = suffix();
        Branch branch = createBranch("Admin Menu Branch " + suffix);
        MenuCategory category = menuCategoryRepository.saveAndFlush(new MenuCategory(
                nextId("menu_category", "id"),
                "Admin Menu Item Category " + suffix,
                1
        ));
        MenuCategory otherCategory = menuCategoryRepository.saveAndFlush(new MenuCategory(
                nextId("menu_category", "id"),
                "Admin Other Menu Category " + suffix,
                1
        ));
        menuItemRepository.saveAndFlush(new MenuItem(
                nextId("menu_item", "id"), otherCategory, "Existing high-order item " + suffix,
                null, 99, false, false
        ));
        ModifierGroup modifierGroup = modifierGroupRepository.saveAndFlush(new ModifierGroup(
                nextId("modifier_group", "id"),
                "Admin Modifier Group " + suffix,
                false,
                0,
                2
        ));
        Map<String, String> params = new HashMap<>();
        params.put("active", "on");
        params.put("is300ml", "on");
        branchRepository.findAll().forEach(currentBranch -> params.put(
                "menuPrice_" + currentBranch.getId(),
                currentBranch.getId().equals(branch.getId()) ? "64.50" : "0.00"
        ));

        catalogAdminService.saveMenuItem(
                null,
                "  Admin Menu Item " + suffix + "  ",
                category.getId(),
                "  Saved from service  ",
                List.of(modifierGroup.getId()),
                params,
                null
        );
        MenuItem saved = findMenuItem("Admin Menu Item " + suffix);

        assertThat(saved.getDescription()).isEqualTo("Saved from service");
        assertThat(saved.getSortOrder()).isEqualTo(1);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.isIs300ml()).isTrue();
        assertThat(branchMenuItemPriceRepository.findByBranchIdAndMenuItemId(branch.getId(), saved.getId()))
                .map(BranchMenuItemPrice::getPrice)
                .contains(64.50);
        for (Branch otherBranch : branchRepository.findAll()) {
            if (!otherBranch.getId().equals(branch.getId())) {
                assertThat(branchMenuItemPriceRepository.findByBranchIdAndMenuItemId(otherBranch.getId(), saved.getId()))
                        .map(BranchMenuItemPrice::getPrice)
                        .contains(0.0);
            }
        }
        assertThat(menuItemModifierGroupRepository.findByMenuItemId(saved.getId()))
                .extracting(MenuItemModifierGroup::getGroupId)
                .containsExactly(modifierGroup.getId());

        int originalSortOrder = saved.getSortOrder();
        catalogAdminService.saveMenuItem(
                saved.getId(),
                "Updated Admin Menu Item " + suffix,
                otherCategory.getId(),
                "Updated description",
                List.of(modifierGroup.getId()),
                params,
                null
        );
        assertThat(menuItemRepository.findById(saved.getId()).orElseThrow().getSortOrder())
                .isEqualTo(originalSortOrder);
        assertThat(menuItemRepository.findById(saved.getId()).orElseThrow().getCategory().getId())
                .isEqualTo(otherCategory.getId());
    }

    @Test
    void savePizzaPersistsDefaultIngredientsAllowedSizesAndBranchPrices() {
        String suffix = suffix();
        Branch branch = createBranch("Admin Pizza Branch " + suffix);
        PizzaCategory category = pizzaCategoryRepository.saveAndFlush(new PizzaCategory(
                nextId("pizza_category", "pizza_category_id"),
                "Admin Pizza Category " + suffix,
                1
        ));
        PizzaCategory otherCategory = pizzaCategoryRepository.saveAndFlush(new PizzaCategory(
                nextId("pizza_category", "pizza_category_id"),
                "Admin Other Pizza Category " + suffix,
                1
        ));
        pizzaRepository.saveAndFlush(new Pizza(
                nextId("pizza", "pizza_id"), otherCategory, "Existing high-order pizza " + suffix,
                null, 99
        ));
        PizzaSize size = pizzaSizeRepository.saveAndFlush(new PizzaSize(
                nextId("pizza_size", "pizza_size_id"),
                nextId("pizza_size", "cm"),
                1
        ));
        PriceCategory priceCategory = priceCategoryRepository.saveAndFlush(new PriceCategory(
                nextId("price_category", "price_category_id"),
                "Admin Price Category " + suffix,
                1
        ));
        Ingredient ingredient = ingredientRepository.saveAndFlush(new Ingredient(
                nextId("ingredient", "ingredient_id"),
                "Admin Ingredient " + suffix,
                priceCategory,
                false
        ));
        Map<String, String> params = new HashMap<>();
        params.put("active", "on");
        params.put("pizzaSize_" + size.getId(), "on");
        PizzaSize unselectedSize = pizzaSizeRepository.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(size.getId()))
                .findFirst().orElseThrow();
        branchRepository.findAll().forEach(currentBranch -> {
            params.put("pizzaPrice_" + currentBranch.getId() + "_" + size.getId(),
                    currentBranch.getId().equals(branch.getId()) ? "142.75" : "123.45");
            params.put("pizzaPrice_" + currentBranch.getId() + "_" + unselectedSize.getId(), "50.00");
        });

        catalogAdminService.savePizza(
                null,
                "  Admin Pizza " + suffix + "  ",
                category.getId(),
                "  Saved pizza  ",
                List.of(ingredient.getId()),
                params,
                null
        );
        Pizza saved = findPizza("Admin Pizza " + suffix);

        assertThat(saved.getDescription()).isEqualTo("Saved pizza");
        assertThat(saved.getSortOrder()).isEqualTo(1);
        assertThat(saved.isActive()).isTrue();
        assertThat(pizzaDefaultIngredientRepository.findByPizzaId(saved.getId()))
                .extracting(PizzaDefaultIngredient::getIngredient)
                .extracting(Ingredient::getId)
                .containsExactly(ingredient.getId());
        assertThat(branchPizzaPriceRepository.findByBranchIdAndPizzaIdAndPizzaSizeId(branch.getId(), saved.getId(), size.getId()))
                .map(price -> price.getPrice())
                .contains(142.75);
        for (Branch otherBranch : branchRepository.findAll()) {
            if (!otherBranch.getId().equals(branch.getId())) {
                assertThat(branchPizzaPriceRepository.findByBranchIdAndPizzaIdAndPizzaSizeId(
                        otherBranch.getId(), saved.getId(), size.getId()))
                        .map(price -> price.getPrice()).contains(123.45);
            }
            assertThat(branchPizzaPriceRepository.findByBranchIdAndPizzaIdAndPizzaSizeId(
                    otherBranch.getId(), saved.getId(), unselectedSize.getId())).isEmpty();
        }
        assertThat(pizzaAllowedSizeRepository.existsById(new PizzaAllowedSize.PizzaAllowedSizeId(saved.getId(), size.getId())))
                .isTrue();
        assertThat(pizzaAllowedSizeRepository.existsById(new PizzaAllowedSize.PizzaAllowedSizeId(saved.getId(), unselectedSize.getId())))
                .isFalse();

        int originalSortOrder = saved.getSortOrder();
        catalogAdminService.savePizza(
                saved.getId(),
                "Updated Admin Pizza " + suffix,
                otherCategory.getId(),
                "Updated pizza",
                List.of(ingredient.getId()),
                params,
                null
        );
        assertThat(pizzaRepository.findById(saved.getId()).orElseThrow().getSortOrder())
                .isEqualTo(originalSortOrder);
        assertThat(pizzaRepository.findById(saved.getId()).orElseThrow().getCategory().getId())
                .isEqualTo(otherCategory.getId());
    }

    @Test
    void newSortableTaxonomyRecordsUseNextTableOrder() {
        String suffix = suffix();

        int expectedPizzaCategoryOrder = pizzaCategoryRepository.findAll().stream()
                .mapToInt(category -> category.getSortOrder() == null ? 0 : category.getSortOrder())
                .max().orElse(0) + 1;
        catalogAdminService.savePizzaCategory(null, "Auto Pizza Category " + suffix, Map.of("active", "on"), null);
        PizzaCategory pizzaCategory = pizzaCategoryRepository.findAll().stream()
                .filter(category -> category.getName().equals("Auto Pizza Category " + suffix))
                .findFirst().orElseThrow();
        assertThat(pizzaCategory.getSortOrder()).isEqualTo(expectedPizzaCategoryOrder);

        int expectedSizeOrder = pizzaSizeRepository.findAll().stream()
                .mapToInt(size -> size.getSortOrder() == null ? 0 : size.getSortOrder())
                .max().orElse(0) + 1;
        catalogAdminService.savePizzaSize(null, nextId("pizza_size", "cm"), Map.of("active", "on"), null);
        PizzaSize pizzaSize = pizzaSizeRepository.findAll().stream()
                .filter(size -> size.getSortOrder() == expectedSizeOrder)
                .findFirst().orElseThrow();
        assertThat(pizzaSize.getSortOrder()).isEqualTo(expectedSizeOrder);

        int expectedPriceCategoryOrder = priceCategoryRepository.findAll().stream()
                .mapToInt(category -> category.getSortOrder() == null ? 0 : category.getSortOrder())
                .max().orElse(0) + 1;
        catalogAdminService.savePriceCategory(null, "Auto Price Category " + suffix,
                Map.of("active", "on"), null);
        PriceCategory priceCategory = priceCategoryRepository.findAll().stream()
                .filter(category -> category.getName().equals("Auto Price Category " + suffix))
                .findFirst().orElseThrow();
        assertThat(priceCategory.getSortOrder()).isEqualTo(expectedPriceCategoryOrder);
    }

    @Test
    void newMenuItemRequiresValidPriceForEveryBranch() {
        String suffix = suffix();
        MenuCategory category = menuCategoryRepository.saveAndFlush(new MenuCategory(
                nextId("menu_category", "id"), "Price validation menu " + suffix, 1));
        Branch branch = branchRepository.findAll().get(0);
        Map<String, String> validPrices = new HashMap<>();
        branchRepository.findAll().forEach(currentBranch ->
                validPrices.put("menuPrice_" + currentBranch.getId(), "10.00"));

        for (String invalid : List.of("missing", "-1.00", "not-a-price", "1.234")) {
            Map<String, String> params = new HashMap<>(validPrices);
            if ("missing".equals(invalid)) {
                params.remove("menuPrice_" + branch.getId());
            } else {
                params.put("menuPrice_" + branch.getId(), invalid);
            }
            String name = "Unpriced menu " + invalid + suffix;
            assertThatThrownBy(() -> catalogAdminService.saveMenuItem(
                    null, name, category.getId(), null, null, params, null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(menuItemRepository.findAll()).noneMatch(item -> name.equals(item.getName()));
        }
    }

    @Test
    void newPizzaRequiresSelectedSizeAndValidPricesForEveryBranch() {
        String suffix = suffix();
        PizzaCategory category = pizzaCategoryRepository.saveAndFlush(new PizzaCategory(
                nextId("pizza_category", "pizza_category_id"), "Price validation pizza " + suffix, 1));
        PizzaSize size = pizzaSizeRepository.findAll().stream().filter(PizzaSize::isActive).findFirst().orElseThrow();
        Branch branch = branchRepository.findAll().get(0);
        Map<String, String> validPrices = new HashMap<>();
        validPrices.put("pizzaSize_" + size.getId(), "on");
        branchRepository.findAll().forEach(currentBranch ->
                validPrices.put("pizzaPrice_" + currentBranch.getId() + "_" + size.getId(), "50.00"));

        for (String invalid : List.of("no-size", "missing", "-1.00", "not-a-price", "1.234")) {
            Map<String, String> params = new HashMap<>(validPrices);
            String priceKey = "pizzaPrice_" + branch.getId() + "_" + size.getId();
            if ("no-size".equals(invalid)) {
                params.remove("pizzaSize_" + size.getId());
            } else if ("missing".equals(invalid)) {
                params.remove(priceKey);
            } else {
                params.put(priceKey, invalid);
            }
            String name = "Unpriced pizza " + invalid + suffix;
            assertThatThrownBy(() -> catalogAdminService.savePizza(
                    null, name, category.getId(), null, null, params, null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(pizzaRepository.findAll()).noneMatch(pizza -> name.equals(pizza.getName()));
        }
    }

    private Branch createBranch(String name) {
        return branchRepository.saveAndFlush(new Branch(nextId("branch", "branch_id"), name));
    }

    private MenuCategory findMenuCategory(String name) {
        return menuCategoryRepository.findAll().stream()
                .filter(category -> name.equals(category.getName()))
                .findFirst()
                .orElseThrow();
    }

    private MenuItem findMenuItem(String name) {
        return menuItemRepository.findAll().stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElseThrow();
    }

    private Pizza findPizza(String name) {
        return pizzaRepository.findAll().stream()
                .filter(pizza -> name.equals(pizza.getName()))
                .findFirst()
                .orElseThrow();
    }

    private int nextId(String tableName, String columnName) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(" + columnName + "), 0) + 100 FROM " + tableName,
                Integer.class
        );
        return value == null ? 100 : value;
    }

    private String suffix() {
        return UUID.randomUUID().toString();
    }
}
