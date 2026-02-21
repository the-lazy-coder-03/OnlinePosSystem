package org.example.onlinepossystem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.onlinepossystem.entity.*;
import org.example.onlinepossystem.repository.*;
import org.example.onlinepossystem.service.StaffService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class Database implements CommandLineRunner {

    private final DataSource dataSource;
    private final StaffService staffService;
    private final StaffRepository staffRepository;
    private final BranchRepository branchRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final BranchMenuItemPriceRepository branchMenuItemPriceRepository;
    private final BranchPizzaPriceRepository branchPizzaPriceRepository;
    private final PizzaSizeRepository pizzaSizeRepository;
    private final PizzaCategoryRepository pizzaCategoryRepository;
    private final PizzaRepository pizzaRepository;
    private final PizzaAllowedSizeRepository pizzaAllowedSizeRepository;
    private final PriceCategoryRepository priceCategoryRepository;
    private final IngredientRepository ingredientRepository;
    private final PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository;
    private final BranchExtraPriceRepository branchExtraPriceRepository;
    private final BurgerToppingRepository burgerToppingRepository;
    private final SaladIngredientRepository saladIngredientRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Database(DataSource dataSource, StaffService staffService, StaffRepository staffRepository,
                    BranchRepository branchRepository, MenuCategoryRepository menuCategoryRepository,
                    MenuItemRepository menuItemRepository, BranchMenuItemPriceRepository branchMenuItemPriceRepository,
                    BranchPizzaPriceRepository branchPizzaPriceRepository,
                    PizzaSizeRepository pizzaSizeRepository, PizzaCategoryRepository pizzaCategoryRepository,
                    PizzaRepository pizzaRepository,
                    PizzaAllowedSizeRepository pizzaAllowedSizeRepository,
                    PriceCategoryRepository priceCategoryRepository, IngredientRepository ingredientRepository,
                    PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository, BranchExtraPriceRepository branchExtraPriceRepository,
                    BurgerToppingRepository burgerToppingRepository, SaladIngredientRepository saladIngredientRepository) {
        this.dataSource = dataSource;
        this.staffService = staffService;
        this.staffRepository = staffRepository;
        this.branchRepository = branchRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.branchMenuItemPriceRepository = branchMenuItemPriceRepository;
        this.branchPizzaPriceRepository = branchPizzaPriceRepository;
        this.pizzaSizeRepository = pizzaSizeRepository;
        this.pizzaCategoryRepository = pizzaCategoryRepository;
        this.pizzaRepository = pizzaRepository;
        this.pizzaAllowedSizeRepository = pizzaAllowedSizeRepository;
        this.priceCategoryRepository = priceCategoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.pizzaDefaultIngredientRepository = pizzaDefaultIngredientRepository;
        this.branchExtraPriceRepository = branchExtraPriceRepository;
        this.burgerToppingRepository = burgerToppingRepository;
        this.saladIngredientRepository = saladIngredientRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Testing database connection...");

        try (Connection conn = dataSource.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Connected to database successfully!");

                syncStaffFromFile();
                seedMenuData();
            } else {
                System.out.println("❌ Failed to connect to database");
            }
        } catch (SQLException e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
        }
    }

    @Transactional
    public void seedMenuData() {
        if (branchRepository.count() > 0) {
            System.out.println("ℹ️ Database already has branches. Checking if Kenridge/Uitzicht exist...");
            if (branchRepository.findByName("Kenridge").isEmpty()) {
                branchRepository.save(new Branch(1, "Kenridge"));
            }
            if (branchRepository.findByName("Uitzicht").isEmpty()) {
                branchRepository.save(new Branch(2, "Uitzicht"));
            }
            return;
        }

        System.out.println("Seeding pizza data...");

        // 1. Branches
        Branch branchKenridge = branchRepository.save(new Branch(1, "Kenridge"));
        Branch branchUitzicht = branchRepository.save(new Branch(2, "Uitzicht"));

        // 2. Pizza sizes
        PizzaSize size19 = pizzaSizeRepository.save(new PizzaSize(1, 19, 1));
        PizzaSize size23 = pizzaSizeRepository.save(new PizzaSize(2, 23, 2));
        PizzaSize size30 = pizzaSizeRepository.save(new PizzaSize(3, 30, 3));

        // 3. Categories
        PizzaCategory favsP = pizzaCategoryRepository.save(new PizzaCategory(1, "Favourites", 1));
        PizzaCategory supremesP = pizzaCategoryRepository.save(new PizzaCategory(2, "Supremes", 2));

        MenuCategory burgers = menuCategoryRepository.save(new MenuCategory(1, "Burgers", 10));
        MenuCategory drinks = menuCategoryRepository.save(new MenuCategory(4, "Cool Drinks", 40));

        // 4. Price categories
        PriceCategory pc1 = priceCategoryRepository.save(new PriceCategory(1, "Chilli / Garlic", 1));
        PriceCategory pc2 = priceCategoryRepository.save(new PriceCategory(2, "Onion / Green Pepper / Capers / Banana / Fresh Tomato", 2));
        PriceCategory pc3 = priceCategoryRepository.save(new PriceCategory(3, "Olives / Asparagus / Spinach / Mushrooms / Peppadew / Sundried Tomato / Pineapple", 3));
        PriceCategory pc4 = priceCategoryRepository.save(new PriceCategory(4, "All Cheeses / Meats / Avo (seasonal)", 4));

        // 5. Ingredients
        Ingredient i1 = ingredientRepository.save(new Ingredient(1, "chilli", pc1, false));
        Ingredient i2 = ingredientRepository.save(new Ingredient(2, "garlic", pc1, false));
        Ingredient i15 = ingredientRepository.save(new Ingredient(15, "mozzarella", pc4, false));
        Ingredient i28 = ingredientRepository.save(new Ingredient(28, "tomato base", pc2, false));

        // 6. Pizzas
        Pizza p101 = pizzaRepository.save(new Pizza(101, favsP, "Garlic Pita", "fresh garlic & origanum", 1));
        Pizza p103 = pizzaRepository.save(new Pizza(103, favsP, "Margherita", "tomato base and mozzarella cheese", 3));

        // 7. Menu Items
        MenuItem m101 = menuItemRepository.save(new MenuItem(101, drinks, "Coke 300ml", "Coke can", 1, true, false));
        MenuItem m201 = menuItemRepository.save(new MenuItem(201, burgers, "Cheese Burger", "Single patty burger", 1, false, false));

        // 8. Allowed Sizes
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p101, size23));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p101, size30));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p103, size19));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p103, size23));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p103, size30));

        // 9. Recipes
        pizzaDefaultIngredientRepository.save(new PizzaDefaultIngredient(p103, i28, true, 1, 1));
        pizzaDefaultIngredientRepository.save(new PizzaDefaultIngredient(p103, i15, true, 1, 2));

        // 10. Prices
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branchKenridge, m101, 18.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branchUitzicht, m101, 16.0));

        branchPizzaPriceRepository.save(new BranchPizzaPrice(branchKenridge, p101, size23, 59.0));
        branchPizzaPriceRepository.save(new BranchPizzaPrice(branchKenridge, p101, size30, 78.0));
        branchPizzaPriceRepository.save(new BranchPizzaPrice(branchKenridge, p103, size19, 58.0));
        branchPizzaPriceRepository.save(new BranchPizzaPrice(branchKenridge, p103, size23, 92.0));
        branchPizzaPriceRepository.save(new BranchPizzaPrice(branchKenridge, p103, size30, 112.0));

        saveExtraPrices(branchKenridge, pc1, pc2, pc3, pc4, size19, size23, size30);
        saveExtraPrices(branchUitzicht, pc1, pc2, pc3, pc4, size19, size23, size30);

        System.out.println("✅ Database seeded with minimal set for testing!");
    }

    private void saveExtraPrices(Branch branch, PriceCategory pc1, PriceCategory pc2, PriceCategory pc3, PriceCategory pc4,
                                PizzaSize s19, PizzaSize s23, PizzaSize s30) {
        branchExtraPriceRepository.save(new BranchExtraPrice(branch, pc1, s19, 7.0));
        branchExtraPriceRepository.save(new BranchExtraPrice(branch, pc1, s23, 8.0));
        branchExtraPriceRepository.save(new BranchExtraPrice(branch, pc1, s30, 9.0));

        branchExtraPriceRepository.save(new BranchExtraPrice(branch, pc2, s19, 14.0));
        branchExtraPriceRepository.save(new BranchExtraPrice(branch, pc2, s23, 17.0));
        branchExtraPriceRepository.save(new BranchExtraPrice(branch, pc2, s30, 18.0));

        branchExtraPriceRepository.save(new BranchExtraPrice(branch, pc3, s19, 16.0));
        branchExtraPriceRepository.save(new BranchExtraPrice(branch, pc3, s23, 20.0));
        branchExtraPriceRepository.save(new BranchExtraPrice(branch, pc3, s30, 22.0));

        branchExtraPriceRepository.save(new BranchExtraPrice(branch, pc4, s19, 17.0));
        branchExtraPriceRepository.save(new BranchExtraPrice(branch, pc4, s23, 22.0));
        branchExtraPriceRepository.save(new BranchExtraPrice(branch, pc4, s30, 25.0));
    }

    private void syncStaffFromFile() {
        File configFile = new File("staff-config.json");
        if (!configFile.exists()) {
            System.out.println("ℹ️ No staff-config.json found in root directory. Skipping staff sync.");
            return;
        }

        try {
            System.out.println("Reading staff configuration from staff-config.json...");
            List<Map<String, String>> staffConfigs = objectMapper.readValue(
                    configFile,
                    new TypeReference<List<Map<String, String>>>() {}
            );

            for (Map<String, String> config : staffConfigs) {
                String name = config.get("name");
                String branch = config.get("branch");
                String pin = config.get("pin");
                String branchCode = config.get("branchCode");

                if (name != null && branch != null) {
                    staffService.updateOrCreateStaff(name, branch, pin, branchCode);
                    System.out.println("Synced staff for branch: " + branch);
                }
            }
            System.out.println("✅ Staff sync complete!");
        } catch (Exception e) {
            System.err.println("❌ Failed to sync staff from file : " + e.getMessage());
        }
    }
}
