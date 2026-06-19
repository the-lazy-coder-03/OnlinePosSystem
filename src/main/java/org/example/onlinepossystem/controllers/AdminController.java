package org.example.onlinepossystem.controllers;

import org.example.onlinepossystem.entity.Branch;
import org.example.onlinepossystem.entity.BranchExtraPrice;
import org.example.onlinepossystem.entity.BranchMenuItemPrice;
import org.example.onlinepossystem.entity.BranchPizzaPrice;
import org.example.onlinepossystem.entity.Ingredient;
import org.example.onlinepossystem.entity.MenuCategory;
import org.example.onlinepossystem.entity.MenuItem;
import org.example.onlinepossystem.entity.MenuItemModifierGroup;
import org.example.onlinepossystem.entity.ModifierGroup;
import org.example.onlinepossystem.entity.ModifierOption;
import org.example.onlinepossystem.entity.Pizza;
import org.example.onlinepossystem.entity.PizzaAllowedSize;
import org.example.onlinepossystem.entity.PizzaCategory;
import org.example.onlinepossystem.entity.PizzaDefaultIngredient;
import org.example.onlinepossystem.entity.PizzaSize;
import org.example.onlinepossystem.entity.PriceCategory;
import org.example.onlinepossystem.repository.BranchExtraPriceRepository;
import org.example.onlinepossystem.repository.BranchMenuItemPriceRepository;
import org.example.onlinepossystem.repository.BranchPizzaPriceRepository;
import org.example.onlinepossystem.repository.BranchRepository;
import org.example.onlinepossystem.repository.IngredientRepository;
import org.example.onlinepossystem.repository.MenuCategoryRepository;
import org.example.onlinepossystem.repository.MenuItemModifierGroupRepository;
import org.example.onlinepossystem.repository.MenuItemRepository;
import org.example.onlinepossystem.repository.ModifierGroupRepository;
import org.example.onlinepossystem.repository.ModifierOptionRepository;
import org.example.onlinepossystem.repository.PizzaAllowedSizeRepository;
import org.example.onlinepossystem.repository.PizzaCategoryRepository;
import org.example.onlinepossystem.repository.PizzaDefaultIngredientRepository;
import org.example.onlinepossystem.repository.PizzaRepository;
import org.example.onlinepossystem.repository.PizzaSizeRepository;
import org.example.onlinepossystem.repository.PriceCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

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
    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierOptionRepository modifierOptionRepository;
    private final MenuItemModifierGroupRepository menuItemModifierGroupRepository;

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
                           PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository,
                           ModifierGroupRepository modifierGroupRepository,
                           ModifierOptionRepository modifierOptionRepository,
                           MenuItemModifierGroupRepository menuItemModifierGroupRepository) {
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
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierOptionRepository = modifierOptionRepository;
        this.menuItemModifierGroupRepository = menuItemModifierGroupRepository;
    }

    @GetMapping
    public String adminDashboard(Model model) {
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

        model.addAttribute("pizzas", pizzas);
        model.addAttribute("menuItems", menuItems);
        model.addAttribute("branches", sorted(branchRepository.findAll(), Comparator.comparing(Branch::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))));
        model.addAttribute("pizzaCategories", pizzaCategories);
        model.addAttribute("menuCategories", menuCategories);
        model.addAttribute("pizzaSizes", pizzaSizes);
        List<BranchPizzaPrice> branchPizzaPrices = branchPizzaPriceRepository.findAll();
        List<BranchMenuItemPrice> branchMenuItemPrices = branchMenuItemPriceRepository.findAll();
        List<PizzaDefaultIngredient> pizzaDefaultIngredients = pizzaDefaultIngredientRepository.findAll();
        List<MenuItemModifierGroup> menuItemModifierGroups = menuItemModifierGroupRepository.findAll();

        model.addAttribute("branchPizzaPrices", branchPizzaPrices);
        model.addAttribute("branchMenuItemPrices", branchMenuItemPrices);
        model.addAttribute("priceCategories", priceCategories);
        model.addAttribute("branchExtraPrices", branchExtraPriceRepository.findAll());
        model.addAttribute("ingredients", ingredients);
        model.addAttribute("pizzaDefaultIngredients", pizzaDefaultIngredients);
        model.addAttribute("modifierGroups", modifierGroups);
        model.addAttribute("modifierOptions", modifierOptions);
        model.addAttribute("menuItemModifierGroups", menuItemModifierGroups);
        model.addAttribute("pizzaIngredientIdsByPizzaId", pizzaDefaultIngredients.stream()
                .filter(pdi -> pdi.getPizza() != null && pdi.getIngredient() != null)
                .collect(Collectors.groupingBy(
                        pdi -> pdi.getPizza().getId(),
                        Collectors.mapping(pdi -> String.valueOf(pdi.getIngredient().getId()), Collectors.joining(",")))));
        model.addAttribute("modifierGroupIdsByMenuItemId", menuItemModifierGroups.stream()
                .collect(Collectors.groupingBy(
                        MenuItemModifierGroup::getMenuItemId,
                        Collectors.mapping(link -> String.valueOf(link.getGroupId()), Collectors.joining(",")))));
        model.addAttribute("activePizzaCount", pizzas.stream().filter(Pizza::isActive).count());
        model.addAttribute("activeMenuItemCount", menuItems.stream().filter(MenuItem::isActive).count());
        model.addAttribute("activeIngredientCount", ingredients.stream().filter(Ingredient::isActive).count());
        return "admin";
    }

    @GetMapping("/orders")
    public String adminOrdersPage(Model model) {
        model.addAttribute("adminMode", true);
        return "InputOrders";
    }

    @PostMapping("/pizzas/save")
    @Transactional
    public String savePizza(@RequestParam(required = false) Integer id,
                            @RequestParam String name,
                            @RequestParam Integer categoryId,
                            @RequestParam(required = false) String description,
                            @RequestParam(required = false) Integer sortOrder,
                            @RequestParam(required = false) List<Integer> ingredientIds,
                            @RequestParam Map<String, String> params,
                            Authentication authentication) {
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

        logger.info("Admin action={} pizzaId={} admin={}", "savePizza", savedPizza.getId(), adminName(authentication));
        return "redirect:/admin#items";
    }

    @PostMapping("/pizzas/delete/{id}")
    public String deletePizza(@PathVariable Integer id, Authentication authentication) {
        pizzaRepository.deleteById(id);
        logger.info("Admin action={} pizzaId={} admin={}", "deletePizza", id, adminName(authentication));
        return "redirect:/admin#items";
    }

    @PostMapping("/pizzas/price")
    public String updatePizzaPrice(@RequestParam Integer branchId,
                                   @RequestParam Integer pizzaId,
                                   @RequestParam Integer pizzaSizeId,
                                   @RequestParam Double price,
                                   Authentication authentication) {
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new java.util.NoSuchElementException("Branch not found with ID: " + branchId));
        Pizza pizza = pizzaRepository.findById(pizzaId).orElseThrow(() -> new java.util.NoSuchElementException("Pizza not found with ID: " + pizzaId));
        PizzaSize pizzaSize = pizzaSizeRepository.findById(pizzaSizeId).orElseThrow(() -> new java.util.NoSuchElementException("Pizza size not found with ID: " + pizzaSizeId));
        savePizzaPrice(branch, pizza, pizzaSize, price);

        logger.info("Admin action={} branchId={} pizzaId={} sizeId={} admin={}",
                "updatePizzaPrice", branchId, pizzaId, pizzaSizeId, adminName(authentication));
        return "redirect:/admin#pricing";
    }

    @PostMapping("/menu-items/save")
    @Transactional
    public String saveMenuItem(@RequestParam(required = false) Integer id,
                               @RequestParam String name,
                               @RequestParam Integer categoryId,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) Integer sortOrder,
                               @RequestParam(required = false) List<Integer> modifierGroupIds,
                               @RequestParam Map<String, String> params,
                               Authentication authentication) {
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

        logger.info("Admin action={} menuItemId={} admin={}", "saveMenuItem", savedMenuItem.getId(), adminName(authentication));
        return "redirect:/admin#items";
    }

    @PostMapping("/menu-items/delete/{id}")
    public String deleteMenuItem(@PathVariable Integer id, Authentication authentication) {
        menuItemRepository.deleteById(id);
        logger.info("Admin action={} menuItemId={} admin={}", "deleteMenuItem", id, adminName(authentication));
        return "redirect:/admin#items";
    }

    @PostMapping("/menu-items/price")
    public String updateMenuItemPrice(@RequestParam Integer branchId,
                                      @RequestParam Integer menuItemId,
                                      @RequestParam Double price,
                                      Authentication authentication) {
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new java.util.NoSuchElementException("Branch not found with ID: " + branchId));
        MenuItem menuItem = menuItemRepository.findById(menuItemId).orElseThrow(() -> new java.util.NoSuchElementException("Menu item not found with ID: " + menuItemId));

        saveMenuItemPrice(branch, menuItem, price);
        logger.info("Admin action={} branchId={} menuItemId={} admin={}",
                "updateMenuItemPrice", branchId, menuItemId, adminName(authentication));
        return "redirect:/admin#pricing";
    }

    @PostMapping("/toppings/price")
    public String updateToppingPrice(@RequestParam Integer branchId,
                                     @RequestParam Integer priceCategoryId,
                                     @RequestParam Integer pizzaSizeId,
                                     @RequestParam Double price,
                                     Authentication authentication) {
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new java.util.NoSuchElementException("Branch not found with ID: " + branchId));
        PriceCategory priceCategory = priceCategoryRepository.findById(priceCategoryId).orElseThrow(() -> new java.util.NoSuchElementException("Price category not found with ID: " + priceCategoryId));
        PizzaSize pizzaSize = pizzaSizeRepository.findById(pizzaSizeId).orElseThrow(() -> new java.util.NoSuchElementException("Pizza size not found with ID: " + pizzaSizeId));

        BranchExtraPrice bep = branchExtraPriceRepository.findById(new BranchExtraPrice.BranchExtraPriceId(branchId, priceCategoryId, pizzaSizeId))
                .orElse(new BranchExtraPrice(branch, priceCategory, pizzaSize, price));
        bep.setPrice(price);
        branchExtraPriceRepository.save(bep);
        logger.info("Admin action={} branchId={} priceCategoryId={} sizeId={} admin={}",
                "updateToppingPrice", branchId, priceCategoryId, pizzaSizeId, adminName(authentication));
        return "redirect:/admin#pricing";
    }

    @PostMapping("/pizza-categories/save")
    public String savePizzaCategory(@RequestParam(required = false) Integer id,
                                    @RequestParam String name,
                                    @RequestParam(required = false) Integer sortOrder,
                                    @RequestParam Map<String, String> params,
                                    Authentication authentication) {
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
        logger.info("Admin action={} pizzaCategoryId={} admin={}", "savePizzaCategory", category.getId(), adminName(authentication));
        return "redirect:/admin#categories";
    }

    @PostMapping("/menu-categories/save")
    public String saveMenuCategory(@RequestParam(required = false) Integer id,
                                   @RequestParam String name,
                                   @RequestParam(required = false) Integer sortOrder,
                                   @RequestParam Map<String, String> params,
                                   Authentication authentication) {
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
        logger.info("Admin action={} menuCategoryId={} admin={}", "saveMenuCategory", category.getId(), adminName(authentication));
        return "redirect:/admin#categories";
    }

    @PostMapping("/pizza-sizes/save")
    public String savePizzaSize(@RequestParam(required = false) Integer id,
                                @RequestParam Integer cm,
                                @RequestParam(required = false) Integer sortOrder,
                                @RequestParam Map<String, String> params,
                                Authentication authentication) {
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
        logger.info("Admin action={} pizzaSizeId={} admin={}", "savePizzaSize", size.getId(), adminName(authentication));
        return "redirect:/admin#categories";
    }

    @PostMapping("/price-categories/save")
    public String savePriceCategory(@RequestParam(required = false) Integer id,
                                    @RequestParam String name,
                                    @RequestParam(required = false) Integer sortOrder,
                                    @RequestParam Map<String, String> params,
                                    Authentication authentication) {
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
        logger.info("Admin action={} priceCategoryId={} admin={}", "savePriceCategory", category.getId(), adminName(authentication));
        return "redirect:/admin#categories";
    }

    @PostMapping("/ingredients/save")
    public String saveIngredient(@RequestParam(required = false) Integer id,
                                 @RequestParam String name,
                                 @RequestParam Integer priceCategoryId,
                                 @RequestParam Map<String, String> params,
                                 Authentication authentication) {
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
        logger.info("Admin action={} ingredientId={} admin={}", "saveIngredient", ingredient.getId(), adminName(authentication));
        return "redirect:/admin#ingredients";
    }

    @PostMapping("/modifier-groups/save")
    public String saveModifierGroup(@RequestParam(required = false) Integer id,
                                    @RequestParam String name,
                                    @RequestParam(required = false) Integer minSelect,
                                    @RequestParam(required = false) Integer maxSelect,
                                    @RequestParam Map<String, String> params,
                                    Authentication authentication) {
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
        logger.info("Admin action={} modifierGroupId={} admin={}", "saveModifierGroup", group.getId(), adminName(authentication));
        return "redirect:/admin#modifiers";
    }

    @PostMapping("/modifier-options/save")
    public String saveModifierOption(@RequestParam(required = false) Integer id,
                                     @RequestParam Integer groupId,
                                     @RequestParam String name,
                                     @RequestParam(required = false) String menuItemId,
                                     Authentication authentication) {
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
        modifierOptionRepository.save(option);
        logger.info("Admin action={} modifierOptionId={} admin={}", "saveModifierOption", option.getId(), adminName(authentication));
        return "redirect:/admin#modifiers";
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
        for (Branch branch : branchRepository.findAll()) {
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
        for (Branch branch : branchRepository.findAll()) {
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

    private String adminName(Authentication authentication) {
        return authentication == null ? "unknown" : authentication.getName();
    }
}
