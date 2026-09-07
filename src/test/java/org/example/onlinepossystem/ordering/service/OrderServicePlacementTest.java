package org.example.onlinepossystem.ordering.service;

import jakarta.persistence.EntityManager;
import org.example.onlinepossystem.branch.entity.Branch;
import org.example.onlinepossystem.branch.repository.BranchRepository;
import org.example.onlinepossystem.catalog.entity.BranchExtraPrice;
import org.example.onlinepossystem.catalog.entity.BranchMenuItemPrice;
import org.example.onlinepossystem.catalog.entity.BranchPizzaPrice;
import org.example.onlinepossystem.catalog.entity.Ingredient;
import org.example.onlinepossystem.catalog.entity.MenuCategory;
import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.entity.ModifierGroup;
import org.example.onlinepossystem.catalog.entity.ModifierOption;
import org.example.onlinepossystem.catalog.entity.Pizza;
import org.example.onlinepossystem.catalog.entity.PizzaAllowedSize;
import org.example.onlinepossystem.catalog.entity.PizzaCategory;
import org.example.onlinepossystem.catalog.entity.PizzaSize;
import org.example.onlinepossystem.catalog.entity.PriceCategory;
import org.example.onlinepossystem.catalog.repository.BranchExtraPriceRepository;
import org.example.onlinepossystem.catalog.repository.BranchMenuItemPriceRepository;
import org.example.onlinepossystem.catalog.repository.BranchPizzaPriceRepository;
import org.example.onlinepossystem.catalog.repository.IngredientRepository;
import org.example.onlinepossystem.catalog.repository.MenuCategoryRepository;
import org.example.onlinepossystem.catalog.repository.MenuItemRepository;
import org.example.onlinepossystem.catalog.repository.ModifierGroupRepository;
import org.example.onlinepossystem.catalog.repository.ModifierOptionRepository;
import org.example.onlinepossystem.catalog.repository.PizzaAllowedSizeRepository;
import org.example.onlinepossystem.catalog.repository.PizzaCategoryRepository;
import org.example.onlinepossystem.catalog.repository.PizzaRepository;
import org.example.onlinepossystem.catalog.repository.PizzaSizeRepository;
import org.example.onlinepossystem.catalog.repository.PriceCategoryRepository;
import org.example.onlinepossystem.ordering.api.OrderOperations;
import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderServicePlacementTest {
    @Autowired
    private OrderOperations orderOperations;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private BranchMenuItemPriceRepository branchMenuItemPriceRepository;

    @Autowired
    private ModifierGroupRepository modifierGroupRepository;

    @Autowired
    private ModifierOptionRepository modifierOptionRepository;

    @Autowired
    private PizzaCategoryRepository pizzaCategoryRepository;

    @Autowired
    private PizzaRepository pizzaRepository;

    @Autowired
    private PizzaSizeRepository pizzaSizeRepository;

    @Autowired
    private PizzaAllowedSizeRepository pizzaAllowedSizeRepository;

    @Autowired
    private BranchPizzaPriceRepository branchPizzaPriceRepository;

    @Autowired
    private PriceCategoryRepository priceCategoryRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private BranchExtraPriceRepository branchExtraPriceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void placesPizzaItemsWithExtraIngredients() {
        PizzaFixture fixture = createPizzaFixture();

        OrderResponseDTO response = orderOperations.placeOrder(request(
                fixture.branch().getName(),
                pizzaItem(
                        fixture.pizza().getId(),
                        fixture.size().getId(),
                        2,
                        customization(fixture.extraIngredient().getId(), 2, "ingredient")
                )
        ));
        entityManager.flush();

        assertThat(response.getBranchName()).isEqualTo(fixture.branch().getName());
        assertThat(response.getPizzaItems()).hasSize(1);
        OrderResponseDTO.PizzaItemDTO pizzaItem = response.getPizzaItems().get(0);
        assertThat(pizzaItem.getPizzaId()).isEqualTo(fixture.pizza().getId());
        assertThat(pizzaItem.getPizzaSizeId()).isEqualTo(fixture.size().getId());
        assertThat(pizzaItem.getQty()).isEqualTo(2);
        assertThat(pizzaItem.getBasePriceAtTime()).isEqualTo(119.99);
        assertThat(pizzaItem.getExtras()).singleElement().satisfies(extra -> {
            assertThat(extra.getIngredientId()).isEqualTo(fixture.extraIngredient().getId());
            assertThat(extra.getIngredientName()).isEqualTo(fixture.extraIngredient().getName());
            assertThat(extra.getQty()).isEqualTo(2);
            assertThat(extra.getUnitPriceAtTime()).isEqualTo(12.50);
        });
    }

    @Test
    void placesMenuItemsWithModifierExtras() {
        MenuFixture fixture = createMenuFixture();

        OrderResponseDTO response = orderOperations.placeOrder(request(
                fixture.branch().getName(),
                menuItem(
                        fixture.menuItem().getId(),
                        3,
                        customization(fixture.modifierOption().getId(), 2, "modifierOption")
                )
        ));
        entityManager.flush();

        assertThat(response.getMenuItems()).hasSize(1);
        OrderResponseDTO.MenuItemDTO item = response.getMenuItems().get(0);
        assertThat(item.getMenuItemId()).isEqualTo(fixture.menuItem().getId());
        assertThat(item.getQty()).isEqualTo(3);
        assertThat(item.getUnitPriceAtTime()).isEqualTo(49.95);
        assertThat(item.getExtras()).singleElement().satisfies(extra -> {
            assertThat(extra.getName()).isEqualTo(fixture.modifierOption().getName());
            assertThat(extra.getQty()).isEqualTo(2);
            assertThat(extra.getUnitPriceAtTime()).isEqualTo(8.75);
        });
    }

    @Test
    void placesBurgerItemsWithProteinRemovedDefaultAndExtraComponents() {
        BurgerFixture fixture = createBurgerFixture();

        OrderResponseDTO response = orderOperations.placeOrder(request(
                fixture.branch().getName(),
                menuItem(
                        fixture.burger().getId(),
                        1,
                        customization(fixture.proteinId(), 1, "burgerComponent"),
                        customization(fixture.extraId(), 2, "burgerExtraComponent")
                )
        ));
        entityManager.flush();

        assertThat(response.getMenuItems()).hasSize(1);
        OrderResponseDTO.MenuItemDTO item = response.getMenuItems().get(0);
        assertThat(item.getMenuItemId()).isEqualTo(fixture.burger().getId());
        assertThat(item.getExtras()).extracting(OrderResponseDTO.MenuItemExtraDTO::getName)
                .containsExactly(
                        "Protein: " + fixture.proteinName(),
                        "No " + fixture.defaultName(),
                        "Extra " + fixture.extraName()
                );
        assertThat(item.getExtras()).extracting(OrderResponseDTO.MenuItemExtraDTO::getQty)
                .containsExactly(1, 1, 2);
        assertThat(item.getExtras()).extracting(OrderResponseDTO.MenuItemExtraDTO::getUnitPriceAtTime)
                .containsExactly(11.25, 0.0, 4.50);
    }

    @Test
    void rejectsOrdersForInvalidBranch() {
        OrderRequestDTO request = request(
                "Missing Branch " + suffix(),
                menuItem(1, 1)
        );

        assertThatThrownBy(() -> orderOperations.placeOrder(request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Branch not found");
    }

    @Test
    void rejectsPizzaOrdersForInvalidSize() {
        PizzaFixture fixture = createPizzaFixture();
        int missingSizeId = nextId("pizza_size", "pizza_size_id");

        assertThatThrownBy(() -> orderOperations.placeOrder(request(
                fixture.branch().getName(),
                pizzaItem(fixture.pizza().getId(), missingSizeId, 1)
        )))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Pizza size not found with ID");
    }

    @Test
    void rejectsPizzaOrdersForInvalidCustomizationIds() {
        PizzaFixture fixture = createPizzaFixture();
        int missingIngredientId = nextId("ingredient", "ingredient_id");

        assertThatThrownBy(() -> orderOperations.placeOrder(request(
                fixture.branch().getName(),
                pizzaItem(
                        fixture.pizza().getId(),
                        fixture.size().getId(),
                        1,
                        customization(missingIngredientId, 1, "ingredient")
                )
        )))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Ingredient not found with ID");
    }

    private PizzaFixture createPizzaFixture() {
        String suffix = suffix();
        Branch branch = createBranch("Pizza Branch " + suffix);
        PizzaCategory category = pizzaCategoryRepository.saveAndFlush(new PizzaCategory(
                nextId("pizza_category", "pizza_category_id"),
                "Pizza Category " + suffix,
                1
        ));
        PizzaSize size = pizzaSizeRepository.saveAndFlush(new PizzaSize(
                nextId("pizza_size", "pizza_size_id"),
                nextId("pizza_size", "cm"),
                1
        ));
        PriceCategory priceCategory = priceCategoryRepository.saveAndFlush(new PriceCategory(
                nextId("price_category", "price_category_id"),
                "Extra Price Category " + suffix,
                1
        ));
        Ingredient extraIngredient = ingredientRepository.saveAndFlush(new Ingredient(
                nextId("ingredient", "ingredient_id"),
                "Extra Ingredient " + suffix,
                priceCategory,
                false
        ));
        Pizza pizza = pizzaRepository.saveAndFlush(new Pizza(
                nextId("pizza", "pizza_id"),
                category,
                "Placement Pizza " + suffix,
                "Test pizza",
                1
        ));

        pizzaAllowedSizeRepository.saveAndFlush(new PizzaAllowedSize(pizza, size));
        branchPizzaPriceRepository.saveAndFlush(new BranchPizzaPrice(branch, pizza, size, 119.99));
        branchExtraPriceRepository.saveAndFlush(new BranchExtraPrice(branch, priceCategory, size, 12.50));
        return new PizzaFixture(branch, pizza, size, extraIngredient);
    }

    private MenuFixture createMenuFixture() {
        String suffix = suffix();
        Branch branch = createBranch("Menu Branch " + suffix);
        MenuCategory category = menuCategoryRepository.saveAndFlush(new MenuCategory(
                nextId("menu_category", "id"),
                "Menu Category " + suffix,
                1
        ));
        MenuItem item = menuItemRepository.saveAndFlush(new MenuItem(
                nextId("menu_item", "id"),
                category,
                "Placement Menu Item " + suffix,
                "Test menu item",
                1,
                false,
                false
        ));
        ModifierGroup group = modifierGroupRepository.saveAndFlush(new ModifierGroup(
                nextId("modifier_group", "id"),
                "Sauces " + suffix,
                false,
                0,
                2
        ));
        ModifierOption option = new ModifierOption(
                nextId("modifier_option", "id"),
                group.getId(),
                "Chilli Mayo " + suffix,
                null
        );
        option.setAdditionalPrice(new BigDecimal("8.75"));
        modifierOptionRepository.saveAndFlush(option);
        branchMenuItemPriceRepository.saveAndFlush(new BranchMenuItemPrice(branch, item, 49.95));
        return new MenuFixture(branch, item, option);
    }

    private BurgerFixture createBurgerFixture() {
        String suffix = suffix();
        Branch branch = createBranch("Burger Branch " + suffix);
        MenuCategory category = menuCategoryRepository.saveAndFlush(new MenuCategory(
                nextId("menu_category", "id"),
                "Burger Category " + suffix,
                1
        ));
        MenuItem burger = menuItemRepository.saveAndFlush(new MenuItem(
                nextId("menu_item", "id"),
                category,
                "Placement Burger " + suffix,
                "Test burger",
                1,
                false,
                false
        ));
        branchMenuItemPriceRepository.saveAndFlush(new BranchMenuItemPrice(branch, burger, 89.00));
        entityManager.flush();

        int componentBaseId = nextId("burger_component", "component_id");
        int proteinId = componentBaseId;
        int defaultId = componentBaseId + 1;
        int extraId = componentBaseId + 2;
        int recipeId = nextId("burger_recipe", "recipe_id");
        String proteinName = "Beef Patty " + suffix;
        String defaultName = "Lettuce " + suffix;
        String extraName = "Cheese " + suffix;

        insertBurgerComponent(proteinId, proteinName, "protein");
        insertBurgerComponent(defaultId, defaultName, "default_topping");
        insertBurgerComponent(extraId, extraName, "extra_topping");
        jdbcTemplate.update(
                "INSERT INTO burger_recipe (recipe_id, name, active) VALUES (?, ?, TRUE)",
                recipeId,
                "Standard Test Burger " + suffix
        );
        jdbcTemplate.update(
                "INSERT INTO burger_recipe_component (recipe_id, component_id, is_removable, sort_order) VALUES (?, ?, TRUE, 1)",
                recipeId,
                defaultId
        );
        jdbcTemplate.update(
                "INSERT INTO burger_recipe_assignment (burger_id, recipe_id, protein_quantity_required) VALUES (?, ?, 1)",
                burger.getId(),
                recipeId
        );
        jdbcTemplate.update(
                "INSERT INTO branch_burger_component_price (branch_id, component_id, price) VALUES (?, ?, ?)",
                branch.getId(),
                proteinId,
                new BigDecimal("11.25")
        );
        jdbcTemplate.update(
                "INSERT INTO branch_burger_component_price (branch_id, component_id, price) VALUES (?, ?, ?)",
                branch.getId(),
                extraId,
                new BigDecimal("4.50")
        );

        return new BurgerFixture(branch, burger, proteinId, defaultId, extraId, proteinName, defaultName, extraName);
    }

    private void insertBurgerComponent(int id, String name, String type) {
        jdbcTemplate.update(
                "INSERT INTO burger_component (component_id, name, component_type, active, seasonal) VALUES (?, ?, ?, TRUE, FALSE)",
                id,
                name,
                type
        );
    }

    private Branch createBranch(String name) {
        return branchRepository.saveAndFlush(new Branch(nextId("branch", "branch_id"), name));
    }

    private OrderRequestDTO request(String branchName, OrderRequestDTO.OrderItemRequestDTO... items) {
        OrderRequestDTO request = new OrderRequestDTO();
        request.setCustomerName("Test Customer");
        request.setPhone("0210000000");
        request.setBranchName(branchName);
        request.setOrderType("pickup");
        request.setItems(List.of(items));
        return request;
    }

    private OrderRequestDTO.OrderItemRequestDTO menuItem(
            Integer menuItemId,
            int quantity,
            OrderRequestDTO.CustomizationRequestDTO... customizations
    ) {
        OrderRequestDTO.OrderItemRequestDTO item = new OrderRequestDTO.OrderItemRequestDTO();
        item.setMenuItemId(menuItemId);
        item.setQuantity(quantity);
        item.setCustomizations(List.of(customizations));
        return item;
    }

    private OrderRequestDTO.OrderItemRequestDTO pizzaItem(
            Integer pizzaId,
            Integer pizzaSizeId,
            int quantity,
            OrderRequestDTO.CustomizationRequestDTO... customizations
    ) {
        OrderRequestDTO.OrderItemRequestDTO item = new OrderRequestDTO.OrderItemRequestDTO();
        item.setPizzaId(pizzaId);
        item.setPizzaSizeId(pizzaSizeId);
        item.setQuantity(quantity);
        item.setCustomizations(List.of(customizations));
        return item;
    }

    private OrderRequestDTO.CustomizationRequestDTO customization(Integer id, int quantity, String type) {
        OrderRequestDTO.CustomizationRequestDTO customization = new OrderRequestDTO.CustomizationRequestDTO();
        customization.setId(id);
        customization.setQuantity(quantity);
        customization.setType(type);
        return customization;
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

    private record PizzaFixture(Branch branch, Pizza pizza, PizzaSize size, Ingredient extraIngredient) {
    }

    private record MenuFixture(Branch branch, MenuItem menuItem, ModifierOption modifierOption) {
    }

    private record BurgerFixture(
            Branch branch,
            MenuItem burger,
            int proteinId,
            int defaultId,
            int extraId,
            String proteinName,
            String defaultName,
            String extraName
    ) {
    }
}
