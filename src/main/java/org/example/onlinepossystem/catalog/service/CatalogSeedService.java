package org.example.onlinepossystem.catalog.service;

import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.branch.entity.Branch;
import org.example.onlinepossystem.catalog.entity.BranchExtraPrice;
import org.example.onlinepossystem.catalog.entity.BranchMenuItemPrice;
import org.example.onlinepossystem.catalog.entity.BranchPizzaPrice;
import org.example.onlinepossystem.catalog.entity.Ingredient;
import org.example.onlinepossystem.catalog.entity.MenuCategory;
import org.example.onlinepossystem.catalog.entity.MenuItem;
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
import org.example.onlinepossystem.catalog.repository.MenuItemRepository;
import org.example.onlinepossystem.catalog.repository.PizzaAllowedSizeRepository;
import org.example.onlinepossystem.catalog.repository.PizzaCategoryRepository;
import org.example.onlinepossystem.catalog.repository.PizzaDefaultIngredientRepository;
import org.example.onlinepossystem.catalog.repository.PizzaRepository;
import org.example.onlinepossystem.catalog.repository.PizzaSizeRepository;
import org.example.onlinepossystem.catalog.repository.PriceCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogSeedService {
    private final BranchLookup branchLookup;
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

    public CatalogSeedService(BranchLookup branchLookup,
                              MenuCategoryRepository menuCategoryRepository,
                              MenuItemRepository menuItemRepository,
                              BranchMenuItemPriceRepository branchMenuItemPriceRepository,
                              BranchPizzaPriceRepository branchPizzaPriceRepository,
                              PizzaSizeRepository pizzaSizeRepository,
                              PizzaCategoryRepository pizzaCategoryRepository,
                              PizzaRepository pizzaRepository,
                              PizzaAllowedSizeRepository pizzaAllowedSizeRepository,
                              PriceCategoryRepository priceCategoryRepository,
                              IngredientRepository ingredientRepository,
                              PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository,
                              BranchExtraPriceRepository branchExtraPriceRepository) {
        this.branchLookup = branchLookup;
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
    }

    @Transactional
    public void seedMenuData() {
        if (branchLookup.hasAnyBranches()) {
            System.out.println("ℹ️ Database already has branches. Checking if Kenridge/Uitzicht exist...");
            branchLookup.ensureBranch(1, "Kenridge");
            branchLookup.ensureBranch(2, "Uitzicht");
            return;
        }

        System.out.println("Seeding pizza data...");

        Branch branchKenridge = branchLookup.ensureBranch(1, "Kenridge");
        Branch branchUitzicht = branchLookup.ensureBranch(2, "Uitzicht");

        PizzaSize size19 = pizzaSizeRepository.save(new PizzaSize(1, 19, 1));
        PizzaSize size23 = pizzaSizeRepository.save(new PizzaSize(2, 23, 2));
        PizzaSize size30 = pizzaSizeRepository.save(new PizzaSize(3, 30, 3));

        PizzaCategory favsP = pizzaCategoryRepository.save(new PizzaCategory(1, "Favourites", 1));
        pizzaCategoryRepository.save(new PizzaCategory(2, "Supremes", 2));
        MenuCategory burgers = menuCategoryRepository.save(new MenuCategory(1, "Burgers", 10));
        MenuCategory drinks = menuCategoryRepository.save(new MenuCategory(4, "Cool Drinks", 40));

        PriceCategory pc1 = priceCategoryRepository.save(new PriceCategory(1, "Chilli / Garlic", 1));
        PriceCategory pc2 = priceCategoryRepository.save(new PriceCategory(2, "Onion / Green Pepper / Capers / Banana / Fresh Tomato", 2));
        PriceCategory pc3 = priceCategoryRepository.save(new PriceCategory(3, "Olives / Asparagus / Spinach / Mushrooms / Peppadew / Sundried Tomato / Pineapple", 3));
        PriceCategory pc4 = priceCategoryRepository.save(new PriceCategory(4, "All Cheeses / Meats / Avo (seasonal)", 4));

        Ingredient i1 = ingredientRepository.save(new Ingredient(1, "chilli", pc1, false));
        Ingredient i2 = ingredientRepository.save(new Ingredient(2, "garlic", pc1, false));
        Ingredient i15 = ingredientRepository.save(new Ingredient(15, "mozzarella", pc4, false));
        Ingredient i28 = ingredientRepository.save(new Ingredient(28, "tomato base", pc2, false));

        Pizza p101 = pizzaRepository.save(new Pizza(101, favsP, "Garlic Pita", "fresh garlic & origanum", 1));
        Pizza p103 = pizzaRepository.save(new Pizza(103, favsP, "Margherita", "tomato base and mozzarella cheese", 3));

        MenuItem m101 = menuItemRepository.save(new MenuItem(101, drinks, "Coke 300ml", "Coke can", 1, true, false));
        menuItemRepository.save(new MenuItem(201, burgers, "Cheese Burger", "Single patty burger", 1, false, false));

        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p101, size23));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p101, size30));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p103, size19));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p103, size23));
        pizzaAllowedSizeRepository.save(new PizzaAllowedSize(p103, size30));

        pizzaDefaultIngredientRepository.save(new PizzaDefaultIngredient(p103, i28, true, 1, 1));
        pizzaDefaultIngredientRepository.save(new PizzaDefaultIngredient(p103, i15, true, 1, 2));

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
}
