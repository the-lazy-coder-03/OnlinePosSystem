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

        catalogAdminService.saveMenuCategory(
                null,
                "  Admin Menu Category " + suffix + "  ",
                21,
                Map.of("active", "on"),
                null
        );
        MenuCategory created = findMenuCategory("Admin Menu Category " + suffix);

        catalogAdminService.saveMenuCategory(
                created.getId(),
                "Updated Admin Menu Category " + suffix,
                31,
                Map.of(),
                null
        );
        MenuCategory updated = menuCategoryRepository.findById(created.getId()).orElseThrow();

        assertThat(updated.getName()).isEqualTo("Updated Admin Menu Category " + suffix);
        assertThat(updated.getSortOrder()).isEqualTo(31);
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
        params.put("menuPrice_" + branch.getId(), "64.50");

        catalogAdminService.saveMenuItem(
                null,
                "  Admin Menu Item " + suffix + "  ",
                category.getId(),
                "  Saved from service  ",
                12,
                List.of(modifierGroup.getId()),
                params,
                null
        );
        MenuItem saved = findMenuItem("Admin Menu Item " + suffix);

        assertThat(saved.getDescription()).isEqualTo("Saved from service");
        assertThat(saved.getSortOrder()).isEqualTo(12);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.isIs300ml()).isTrue();
        assertThat(branchMenuItemPriceRepository.findByBranchIdAndMenuItemId(branch.getId(), saved.getId()))
                .map(BranchMenuItemPrice::getPrice)
                .contains(64.50);
        assertThat(menuItemModifierGroupRepository.findByMenuItemId(saved.getId()))
                .extracting(MenuItemModifierGroup::getGroupId)
                .containsExactly(modifierGroup.getId());
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
        params.put("pizzaPrice_" + branch.getId() + "_" + size.getId(), "142.75");

        catalogAdminService.savePizza(
                null,
                "  Admin Pizza " + suffix + "  ",
                category.getId(),
                "  Saved pizza  ",
                9,
                List.of(ingredient.getId()),
                params,
                null
        );
        Pizza saved = findPizza("Admin Pizza " + suffix);

        assertThat(saved.getDescription()).isEqualTo("Saved pizza");
        assertThat(saved.getSortOrder()).isEqualTo(9);
        assertThat(saved.isActive()).isTrue();
        assertThat(pizzaDefaultIngredientRepository.findByPizzaId(saved.getId()))
                .extracting(PizzaDefaultIngredient::getIngredient)
                .extracting(Ingredient::getId)
                .containsExactly(ingredient.getId());
        assertThat(branchPizzaPriceRepository.findByBranchIdAndPizzaIdAndPizzaSizeId(branch.getId(), saved.getId(), size.getId()))
                .map(price -> price.getPrice())
                .contains(142.75);
        assertThat(pizzaAllowedSizeRepository.existsById(new PizzaAllowedSize.PizzaAllowedSizeId(saved.getId(), size.getId())))
                .isTrue();
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
