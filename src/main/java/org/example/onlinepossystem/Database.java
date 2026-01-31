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
    private final PizzaSizeRepository pizzaSizeRepository;
    private final PizzaAllowedSizeRepository pizzaAllowedSizeRepository;
    private final PriceCategoryRepository priceCategoryRepository;
    private final IngredientRepository ingredientRepository;
    private final PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository;
    private final BranchExtraPriceRepository branchExtraPriceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Database(DataSource dataSource, StaffService staffService, StaffRepository staffRepository,
                    BranchRepository branchRepository, MenuCategoryRepository menuCategoryRepository,
                    MenuItemRepository menuItemRepository, BranchMenuItemPriceRepository branchMenuItemPriceRepository,
                    PizzaSizeRepository pizzaSizeRepository, PizzaAllowedSizeRepository pizzaAllowedSizeRepository,
                    PriceCategoryRepository priceCategoryRepository, IngredientRepository ingredientRepository,
                    PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository, BranchExtraPriceRepository branchExtraPriceRepository) {
        this.dataSource = dataSource;
        this.staffService = staffService;
        this.staffRepository = staffRepository;
        this.branchRepository = branchRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.branchMenuItemPriceRepository = branchMenuItemPriceRepository;
        this.pizzaSizeRepository = pizzaSizeRepository;
        this.pizzaAllowedSizeRepository = pizzaAllowedSizeRepository;
        this.priceCategoryRepository = priceCategoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.pizzaDefaultIngredientRepository = pizzaDefaultIngredientRepository;
        this.branchExtraPriceRepository = branchExtraPriceRepository;
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
                branchRepository.save(new Branch(3, "Kenridge"));
            }
            if (branchRepository.findByName("Uitzicht").isEmpty()) {
                branchRepository.save(new Branch(4, "Uitzicht"));
            }
            return;
        }

        System.out.println("Seeding pizza data...");

        // 1. Branches
        Branch branchA = branchRepository.save(new Branch(1, "Branch A"));
        Branch branchB = branchRepository.save(new Branch(2, "Branch B"));
        Branch branchKenridge = branchRepository.save(new Branch(3, "Kenridge"));
        Branch branchUitzicht = branchRepository.save(new Branch(4, "Uitzicht"));

        // 2. Pizza sizes
        PizzaSize size19 = pizzaSizeRepository.save(new PizzaSize(1, 19, 1));
        PizzaSize size23 = pizzaSizeRepository.save(new PizzaSize(2, 23, 2));
        PizzaSize size30 = pizzaSizeRepository.save(new PizzaSize(3, 30, 3));

        // 3. Categories
        MenuCategory favs = menuCategoryRepository.save(new MenuCategory(1, "Favourites", 1));
        MenuCategory supremes = menuCategoryRepository.save(new MenuCategory(2, "Supremes", 2));

        // 4. Price categories
        PriceCategory pc1 = priceCategoryRepository.save(new PriceCategory(1, "Chilli / Garlic", 1));
        PriceCategory pc2 = priceCategoryRepository.save(new PriceCategory(2, "Onion / Green Pepper / Capers / Banana / Fresh Tomato", 2));
        PriceCategory pc3 = priceCategoryRepository.save(new PriceCategory(3, "Olives / Asparagus / Spinach / Mushrooms / Peppadew / Sundried Tomato / Pineapple", 3));
        PriceCategory pc4 = priceCategoryRepository.save(new PriceCategory(4, "All Cheeses / Meats / Avo (seasonal)", 4));

        // 5. Ingredients
        Ingredient i1 = ingredientRepository.save(new Ingredient(1, "chilli", pc1, false));
        Ingredient i2 = ingredientRepository.save(new Ingredient(2, "garlic", pc1, false));
        Ingredient i3 = ingredientRepository.save(new Ingredient(3, "onion", pc2, false));
        Ingredient i4 = ingredientRepository.save(new Ingredient(4, "green pepper", pc2, false));
        Ingredient i5 = ingredientRepository.save(new Ingredient(5, "capers", pc2, false));
        Ingredient i6 = ingredientRepository.save(new Ingredient(6, "banana", pc2, false));
        Ingredient i7 = ingredientRepository.save(new Ingredient(7, "fresh tomato", pc2, false));
        Ingredient i8 = ingredientRepository.save(new Ingredient(8, "olives", pc3, false));
        Ingredient i9 = ingredientRepository.save(new Ingredient(9, "asparagus", pc3, false));
        Ingredient i10 = ingredientRepository.save(new Ingredient(10, "spinach", pc3, false));
        Ingredient i11 = ingredientRepository.save(new Ingredient(11, "mushrooms", pc3, false));
        Ingredient i12 = ingredientRepository.save(new Ingredient(12, "peppadew", pc3, false));
        Ingredient i13 = ingredientRepository.save(new Ingredient(13, "sundried tomato", pc3, false));
        Ingredient i14 = ingredientRepository.save(new Ingredient(14, "pineapple", pc3, false));
        Ingredient i15 = ingredientRepository.save(new Ingredient(15, "mozzarella", pc4, false));
        Ingredient i16 = ingredientRepository.save(new Ingredient(16, "feta", pc4, false));
        Ingredient i17 = ingredientRepository.save(new Ingredient(17, "cheddar", pc4, false));
        Ingredient i18 = ingredientRepository.save(new Ingredient(18, "ham", pc4, false));
        Ingredient i19 = ingredientRepository.save(new Ingredient(19, "bacon", pc4, false));
        Ingredient i20 = ingredientRepository.save(new Ingredient(20, "salami", pc4, false));
        Ingredient i21 = ingredientRepository.save(new Ingredient(21, "chicken", pc4, false));
        Ingredient i22 = ingredientRepository.save(new Ingredient(22, "bbq chicken", pc4, false));
        Ingredient i23 = ingredientRepository.save(new Ingredient(23, "mince", pc4, false));
        Ingredient i24 = ingredientRepository.save(new Ingredient(24, "boerewors", pc4, false));
        Ingredient i25 = ingredientRepository.save(new Ingredient(25, "anchovies", pc4, false));
        Ingredient i26 = ingredientRepository.save(new Ingredient(26, "avo", pc4, true));
        Ingredient i27 = ingredientRepository.save(new Ingredient(27, "origanum", pc1, false));
        Ingredient i28 = ingredientRepository.save(new Ingredient(28, "tomato base", pc2, false));
        Ingredient i29 = ingredientRepository.save(new Ingredient(29, "rib", pc4, false));
        Ingredient i30 = ingredientRepository.save(new Ingredient(30, "rib sauce", pc2, false));
        Ingredient i31 = ingredientRepository.save(new Ingredient(31, "bbq sauce", pc2, false));
        Ingredient i32 = ingredientRepository.save(new Ingredient(32, "tikka sauce", pc2, false));
        Ingredient i33 = ingredientRepository.save(new Ingredient(33, "sweet chilli sauce", pc2, false));
        Ingredient i34 = ingredientRepository.save(new Ingredient(34, "sweet & sour sauce", pc2, false));
        Ingredient i35 = ingredientRepository.save(new Ingredient(35, "honey", pc2, false));
        Ingredient i36 = ingredientRepository.save(new Ingredient(36, "mustard", pc2, false));
        Ingredient i37 = ingredientRepository.save(new Ingredient(37, "mayonnaise", pc2, false));
        Ingredient i38 = ingredientRepository.save(new Ingredient(38, "chutney", pc2, false));
        Ingredient i39 = ingredientRepository.save(new Ingredient(39, "creamy chicken", pc4, false));
        Ingredient i40 = ingredientRepository.save(new Ingredient(40, "shrimps", pc4, false));
        Ingredient i41 = ingredientRepository.save(new Ingredient(41, "mussels", pc4, false));
        Ingredient i42 = ingredientRepository.save(new Ingredient(42, "calamari", pc4, false));
        Ingredient i43 = ingredientRepository.save(new Ingredient(43, "crab sticks", pc4, false));

        // 6. Pizzas
        MenuItem p101 = menuItemRepository.save(new MenuItem(101, favs, "Garlic Pita", "fresh garlic & origanum", 1));
        MenuItem p102 = menuItemRepository.save(new MenuItem(102, favs, "Cheesy Pita", "fresh garlic, origanum & feta or mozzarella", 2));
        MenuItem p103 = menuItemRepository.save(new MenuItem(103, favs, "Margherita", "tomato base and mozzarella cheese", 3));
        MenuItem p104 = menuItemRepository.save(new MenuItem(104, favs, "Regina", "ham & mushrooms", 4));
        MenuItem p105 = menuItemRepository.save(new MenuItem(105, favs, "Hawaiian", "ham & pineapple", 5));
        MenuItem p106 = menuItemRepository.save(new MenuItem(106, favs, "Chicken Delite", "chicken & peppadew", 6));
        MenuItem p107 = menuItemRepository.save(new MenuItem(107, favs, "3 Cheeses", "cheddar, feta & mozzarella", 7));
        MenuItem p108 = menuItemRepository.save(new MenuItem(108, favs, "New Yorker", "bacon, mushrooms & onions", 8));
        MenuItem p109 = menuItemRepository.save(new MenuItem(109, favs, "Caribbean", "bacon, banana & garlic", 9));
        MenuItem p110 = menuItemRepository.save(new MenuItem(110, favs, "Tropical", "bacon & avo", 10));
        MenuItem p111 = menuItemRepository.save(new MenuItem(111, favs, "Mona Lisa", "olives, green peppers, mushrooms & garlic", 11));
        MenuItem p112 = menuItemRepository.save(new MenuItem(112, favs, "Manhattan", "mince, mushrooms & peppadew", 12));
        MenuItem p113 = menuItemRepository.save(new MenuItem(113, favs, "Chicken Fungi", "chicken & mushrooms", 13));
        MenuItem p114 = menuItemRepository.save(new MenuItem(114, favs, "Cosmo", "salami, feta & onion", 14));
        MenuItem p115 = menuItemRepository.save(new MenuItem(115, favs, "Exotica", "cheese, tomato, pineapple & onion", 15));
        MenuItem p116 = menuItemRepository.save(new MenuItem(116, favs, "Salamina", "salami, mushrooms & pineapple", 16));

        MenuItem p201 = menuItemRepository.save(new MenuItem(201, supremes, "Greek", "bacon, spinach, feta & olives", 1));
        MenuItem p202 = menuItemRepository.save(new MenuItem(202, supremes, "A Lotta Meat", "ham, bacon, salami & BBQ chicken", 2));
        MenuItem p203 = menuItemRepository.save(new MenuItem(203, supremes, "Matt's Rib Delight", "rib, onion, pineapple, peppadew & rib sauce", 3));
        MenuItem p204 = menuItemRepository.save(new MenuItem(204, supremes, "Oriental", "bbq chicken, mushrooms, onion, green pepper & bbq sauce", 4));
        MenuItem p205 = menuItemRepository.save(new MenuItem(205, supremes, "Carli's Super", "ham, salami, mushrooms, olives & avo", 5));
        MenuItem p206 = menuItemRepository.save(new MenuItem(206, supremes, "Tikka Chicken", "chicken, onion, peppadew & tikka sauce", 6));
        MenuItem p207 = menuItemRepository.save(new MenuItem(207, supremes, "Mexicana", "mince, onion, green pepper, chilli & garlic", 7));
        MenuItem p208 = menuItemRepository.save(new MenuItem(208, supremes, "Sweet & Sour", "chicken, green pepper, pineapple & sweet & sour sauce", 8));
        MenuItem p209 = menuItemRepository.save(new MenuItem(209, supremes, "Honey & Mustard", "chicken, mushrooms, feta, pineapple & honey and mustard sauce", 9));
        MenuItem p210 = menuItemRepository.save(new MenuItem(210, supremes, "Four Seasons", "salami, olives, mushrooms & asparagus", 10));
        MenuItem p211 = menuItemRepository.save(new MenuItem(211, supremes, "Chicken Mayo", "chicken, onion, mushrooms & mayonnaise", 11));
        MenuItem p212 = menuItemRepository.save(new MenuItem(212, supremes, "Sweet Chilli Chic", "chicken, peppadew, feta & sweet chilli sauce", 12));
        MenuItem p213 = menuItemRepository.save(new MenuItem(213, supremes, "Tahita", "mushrooms, olives, onion, feta & peppadew", 13));
        MenuItem p214 = menuItemRepository.save(new MenuItem(214, supremes, "South African", "boerewors, fresh tomato, mushrooms, onions, garlic & chutney", 14));
        MenuItem p215 = menuItemRepository.save(new MenuItem(215, supremes, "Creamy Chicken", "creamy chicken, mushrooms, asparagus & garlic", 15));
        MenuItem p216 = menuItemRepository.save(new MenuItem(216, supremes, "Fruti Di Mare", "shrimps, mussels, calamari, crab sticks & garlic", 16));
        MenuItem p217 = menuItemRepository.save(new MenuItem(217, supremes, "Al Greeka", "anchovies & olives", 17));

        // 7. Allowed Sizes
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p101, size23));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p101, size30));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p102, size23));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p102, size30));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p103, size19));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p103, size23));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p103, size30));
        // ... (remaining allowed sizes)

        // 8. Recipes (Default ingredients)
        pizzaDefaultIngredientRepository.save(new PizzaDefaultIngredient(p103, i28, true, 1, 1));
        pizzaDefaultIngredientRepository.save(new PizzaDefaultIngredient(p103, i15, true, 1, 2));
        // ... (remaining recipes)

        // 9. Prices (All branches)
        List<Branch> allBranches = List.of(branchA, branchB, branchKenridge, branchUitzicht);
        for (Branch b : allBranches) {
            savePrices(b, size19, size23, size30, 
                       p101, p102, p103, p104, p105, p106, p107, p108, p109, p110, p111, p112, p113, p114, p115, p116,
                       p201, p202, p203, p204, p205, p206, p207, p208, p209, p210, p211, p212, p213, p214, p215, p216, p217);
            saveExtraPrices(b, pc1, pc2, pc3, pc4, size19, size23, size30);
        }

        System.out.println("✅ Pizza data seeded successfully!");
    }

    private void savePrices(Branch branch, PizzaSize s19, PizzaSize s23, PizzaSize s30,
                            MenuItem p101, MenuItem p102, MenuItem p103, MenuItem p104, MenuItem p105, MenuItem p106, 
                            MenuItem p107, MenuItem p108, MenuItem p109, MenuItem p110, MenuItem p111, MenuItem p112, 
                            MenuItem p113, MenuItem p114, MenuItem p115, MenuItem p116,
                            MenuItem p201, MenuItem p202, MenuItem p203, MenuItem p204, MenuItem p205, MenuItem p206, 
                            MenuItem p207, MenuItem p208, MenuItem p209, MenuItem p210, MenuItem p211, MenuItem p212, 
                            MenuItem p213, MenuItem p214, MenuItem p215, MenuItem p216, MenuItem p217) {
        
        // Favourites
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p101, s23, 59.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p101, s30, 78.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p102, s23, 92.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p102, s30, 112.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p103, s19, 58.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p103, s23, 92.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p103, s30, 112.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p104, s19, 64.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p104, s23, 109.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p104, s30, 138.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p105, s19, 64.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p105, s23, 109.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p105, s30, 138.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p106, s23, 109.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p106, s30, 138.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p107, s19, 64.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p107, s23, 109.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p107, s30, 138.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p108, s23, 109.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p108, s30, 138.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p109, s23, 109.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p109, s30, 138.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p110, s19, 64.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p110, s23, 109.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p110, s30, 138.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p111, s23, 109.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p111, s30, 138.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p112, s23, 109.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p112, s30, 138.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p113, s19, 64.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p113, s23, 109.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p113, s30, 138.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p114, s23, 109.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p114, s30, 138.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p115, s23, 109.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p115, s30, 138.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p116, s19, 64.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p116, s23, 109.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p116, s30, 138.0));

        // Supremes
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p201, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p201, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p202, s23, 131.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p202, s30, 159.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p203, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p203, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p204, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p204, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p205, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p205, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p206, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p206, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p207, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p207, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p208, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p208, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p209, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p209, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p210, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p210, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p211, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p211, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p212, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p212, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p213, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p213, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p214, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p214, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p215, s23, 116.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p215, s30, 147.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p216, s30, 152.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p217, s23, 120.0));
        branchMenuItemPriceRepository.save(new BranchMenuItemPrice(branch, p217, s30, 152.0));
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
            System.err.println("❌ Failed to sync staff from file: " + e.getMessage());
        }
    }
}
