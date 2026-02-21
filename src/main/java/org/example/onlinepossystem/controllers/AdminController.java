package org.example.onlinepossystem.controllers;

import org.example.onlinepossystem.entity.*;
import org.example.onlinepossystem.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final PizzaRepository pizzaRepository;
    private final PizzaCategoryRepository pizzaCategoryRepository;
    private final PizzaSizeRepository pizzaSizeRepository;
    private final BranchRepository branchRepository;
    private final BranchPizzaPriceRepository branchPizzaPriceRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final BranchMenuItemPriceRepository branchMenuItemPriceRepository;
    private final PizzaAllowedSizeRepository pizzaAllowedSizeRepository;
    private final PriceCategoryRepository priceCategoryRepository;
    private final BranchExtraPriceRepository branchExtraPriceRepository;
    private final IngredientRepository ingredientRepository;
    private final PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository;

    public AdminController(PizzaRepository pizzaRepository,
                           PizzaCategoryRepository pizzaCategoryRepository,
                           PizzaSizeRepository pizzaSizeRepository,
                           BranchRepository branchRepository,
                           BranchPizzaPriceRepository branchPizzaPriceRepository,
                           MenuItemRepository menuItemRepository,
                           MenuCategoryRepository menuCategoryRepository,
                           BranchMenuItemPriceRepository branchMenuItemPriceRepository,
                           PizzaAllowedSizeRepository pizzaAllowedSizeRepository,
                           PriceCategoryRepository priceCategoryRepository,
                           BranchExtraPriceRepository branchExtraPriceRepository,
                           IngredientRepository ingredientRepository,
                           PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository) {
        this.pizzaRepository = pizzaRepository;
        this.pizzaCategoryRepository = pizzaCategoryRepository;
        this.pizzaSizeRepository = pizzaSizeRepository;
        this.branchRepository = branchRepository;
        this.branchPizzaPriceRepository = branchPizzaPriceRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.branchMenuItemPriceRepository = branchMenuItemPriceRepository;
        this.pizzaAllowedSizeRepository = pizzaAllowedSizeRepository;
        this.priceCategoryRepository = priceCategoryRepository;
        this.branchExtraPriceRepository = branchExtraPriceRepository;
        this.ingredientRepository = ingredientRepository;
        this.pizzaDefaultIngredientRepository = pizzaDefaultIngredientRepository;
    }

    @GetMapping
    public String adminDashboard(Model model) {
        model.addAttribute("pizzas", pizzaRepository.findAll());
        model.addAttribute("menuItems", menuItemRepository.findAll());
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("pizzaCategories", pizzaCategoryRepository.findAll());
        model.addAttribute("menuCategories", menuCategoryRepository.findAll());
        model.addAttribute("pizzaSizes", pizzaSizeRepository.findAll());
        model.addAttribute("branchPizzaPrices", branchPizzaPriceRepository.findAll());
        model.addAttribute("branchMenuItemPrices", branchMenuItemPriceRepository.findAll());
        model.addAttribute("priceCategories", priceCategoryRepository.findAll());
        model.addAttribute("branchExtraPrices", branchExtraPriceRepository.findAll());
        model.addAttribute("ingredients", ingredientRepository.findAllByActiveTrue());
        model.addAttribute("pizzaDefaultIngredients", pizzaDefaultIngredientRepository.findAll());
        return "admin";
    }

    // ====== Pizza CRUD ======

    @PostMapping("/pizzas/save")
    public String savePizza(@ModelAttribute Pizza pizza, 
                            @RequestParam Integer categoryId,
                            @RequestParam(required = false) List<Integer> ingredientIds) {
        pizzaCategoryRepository.findById(categoryId).ifPresent(pizza::setCategory);
        if (pizza.getId() == null) {
            // Find max id and increment
            int maxId = pizzaRepository.findAll().stream()
                    .mapToInt(Pizza::getId)
                    .max()
                    .orElse(0);
            pizza.setId(maxId + 1);
        }
        Pizza savedPizza = pizzaRepository.save(pizza);

        // Update default ingredients
        List<PizzaDefaultIngredient> existing = pizzaDefaultIngredientRepository.findByPizzaId(savedPizza.getId());
        pizzaDefaultIngredientRepository.deleteAll(existing);

        if (ingredientIds != null) {
            for (int i = 0; i < ingredientIds.size(); i++) {
                Integer ingredientId = ingredientIds.get(i);
                final int order = i;
                ingredientRepository.findById(ingredientId).ifPresent(ing -> {
                    pizzaDefaultIngredientRepository.save(new PizzaDefaultIngredient(savedPizza, ing, true, 1, order));
                });
            }
        }
        
        return "redirect:/admin";
    }

    @PostMapping("/pizzas/delete/{id}")
    public String deletePizza(@PathVariable Integer id) {
        pizzaRepository.deleteById(id);
        return "redirect:/admin";
    }

    @PostMapping("/pizzas/price")
    public String updatePizzaPrice(@RequestParam Integer branchId,
                                   @RequestParam Integer pizzaId,
                                   @RequestParam Integer pizzaSizeId,
                                   @RequestParam Double price) {
        Branch branch = branchRepository.findById(branchId).orElseThrow();
        Pizza pizza = pizzaRepository.findById(pizzaId).orElseThrow();
        PizzaSize pizzaSize = pizzaSizeRepository.findById(pizzaSizeId).orElseThrow();

        // 1. Save branch-specific price
        BranchPizzaPrice branchPizzaPrice = branchPizzaPriceRepository.findByBranchIdAndPizzaIdAndPizzaSizeId(branchId, pizzaId, pizzaSizeId)
                .orElse(new BranchPizzaPrice(branch, pizza, pizzaSize, price));
        branchPizzaPrice.setPrice(price);
        branchPizzaPriceRepository.save(branchPizzaPrice);

        // 2. Ensure pizza is marked as allowed for this size (global)
        if (!pizzaAllowedSizeRepository.existsById(new PizzaAllowedSize.PizzaAllowedSizeId(pizzaId, pizzaSizeId))) {
            pizzaAllowedSizeRepository.save(new PizzaAllowedSize(pizza, pizzaSize));
        }

        return "redirect:/admin";
    }

    // ====== MenuItem CRUD ======

    @PostMapping("/menu-items/save")
    public String saveMenuItem(@ModelAttribute MenuItem menuItem, @RequestParam Integer categoryId) {
        menuCategoryRepository.findById(categoryId).ifPresent(menuItem::setCategory);
        if (menuItem.getId() == null) {
            int maxId = menuItemRepository.findAll().stream()
                    .mapToInt(MenuItem::getId)
                    .max()
                    .orElse(0);
            menuItem.setId(maxId + 1);
        }
        menuItemRepository.save(menuItem);
        return "redirect:/admin";
    }

    @PostMapping("/menu-items/delete/{id}")
    public String deleteMenuItem(@PathVariable Integer id) {
        menuItemRepository.deleteById(id);
        return "redirect:/admin";
    }

    @PostMapping("/menu-items/price")
    public String updateMenuItemPrice(@RequestParam Integer branchId,
                                      @RequestParam Integer menuItemId,
                                      @RequestParam Double price) {
        Branch branch = branchRepository.findById(branchId).orElseThrow();
        MenuItem menuItem = menuItemRepository.findById(menuItemId).orElseThrow();

        BranchMenuItemPrice branchMenuItemPrice = branchMenuItemPriceRepository.findByBranchIdAndMenuItemId(branchId, menuItemId)
                .orElse(new BranchMenuItemPrice(branch, menuItem, price));
        branchMenuItemPrice.setPrice(price);
        branchMenuItemPriceRepository.save(branchMenuItemPrice);
        return "redirect:/admin";
    }

    // ====== Extra Topping Pricing ======

    @PostMapping("/toppings/price")
    public String updateToppingPrice(@RequestParam Integer branchId,
                                     @RequestParam Integer priceCategoryId,
                                     @RequestParam Integer pizzaSizeId,
                                     @RequestParam Double price) {
        Branch branch = branchRepository.findById(branchId).orElseThrow();
        PriceCategory priceCategory = priceCategoryRepository.findById(priceCategoryId).orElseThrow();
        PizzaSize pizzaSize = pizzaSizeRepository.findById(pizzaSizeId).orElseThrow();

        BranchExtraPrice bep = branchExtraPriceRepository.findById(new BranchExtraPrice.BranchExtraPriceId(branchId, priceCategoryId, pizzaSizeId))
                .orElse(new BranchExtraPrice(branch, priceCategory, pizzaSize, price));
        bep.setPrice(price);
        branchExtraPriceRepository.save(bep);
        return "redirect:/admin";
    }
}
