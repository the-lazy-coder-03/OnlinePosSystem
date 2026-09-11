package org.example.onlinepossystem.admin.web;

import org.example.onlinepossystem.catalog.api.CatalogAdministration;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final CatalogAdministration catalogAdministration;

    public AdminController(CatalogAdministration catalogAdministration) {
        this.catalogAdministration = catalogAdministration;
    }

    @GetMapping
    public String adminDashboard(Model model) {
        model.addAllAttributes(catalogAdministration.getDashboardAttributes());
        return "admin";
    }

    @GetMapping("/orders")
    public String adminOrdersPage(Model model) {
        model.addAttribute("adminMode", true);
        return "InputOrders";
    }

    @PostMapping("/pizzas/save")
    public String savePizza(@RequestParam(required = false) Integer id,
                            @RequestParam String name,
                            @RequestParam Integer categoryId,
                            @RequestParam(required = false) String description,
                            @RequestParam(required = false) Integer sortOrder,
                            @RequestParam(required = false) List<Integer> ingredientIds,
                            @RequestParam Map<String, String> params,
                            Authentication authentication) {
        catalogAdministration.savePizza(id, name, categoryId, description, sortOrder, ingredientIds, params, actor(authentication));
        return "redirect:/admin#items";
    }

    @PostMapping("/pizzas/delete/{id}")
    public String deletePizza(@PathVariable Integer id, Authentication authentication) {
        catalogAdministration.deletePizza(id, actor(authentication));
        return "redirect:/admin#items";
    }

    @PostMapping("/pizzas/price")
    public String updatePizzaPrice(@RequestParam Integer branchId,
                                   @RequestParam Integer pizzaId,
                                   @RequestParam Integer pizzaSizeId,
                                   @RequestParam Double price,
                                   Authentication authentication) {
        catalogAdministration.updatePizzaPrice(branchId, pizzaId, pizzaSizeId, price, actor(authentication));
        return "redirect:/admin#pricing";
    }

    @PostMapping("/menu-items/save")
    public String saveMenuItem(@RequestParam(required = false) Integer id,
                               @RequestParam String name,
                               @RequestParam Integer categoryId,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) Integer sortOrder,
                               @RequestParam(required = false) List<Integer> modifierGroupIds,
                               @RequestParam Map<String, String> params,
                               Authentication authentication) {
        catalogAdministration.saveMenuItem(id, name, categoryId, description, sortOrder, modifierGroupIds, params, actor(authentication));
        return "redirect:/admin#items";
    }

    @PostMapping("/menu-items/delete/{id}")
    public String deleteMenuItem(@PathVariable Integer id, Authentication authentication) {
        catalogAdministration.deleteMenuItem(id, actor(authentication));
        return "redirect:/admin#items";
    }

    @PostMapping("/menu-items/price")
    public String updateMenuItemPrice(@RequestParam Integer branchId,
                                      @RequestParam Integer menuItemId,
                                      @RequestParam Double price,
                                      Authentication authentication) {
        catalogAdministration.updateMenuItemPrice(branchId, menuItemId, price, actor(authentication));
        return "redirect:/admin#pricing";
    }

    @PostMapping("/toppings/price")
    public String updateToppingPrice(@RequestParam Integer branchId,
                                     @RequestParam Integer priceCategoryId,
                                     @RequestParam Integer pizzaSizeId,
                                     @RequestParam Double price,
                                     Authentication authentication) {
        catalogAdministration.updateToppingPrice(branchId, priceCategoryId, pizzaSizeId, price, actor(authentication));
        return "redirect:/admin#pricing";
    }

    @PostMapping("/pizza-categories/save")
    public String savePizzaCategory(@RequestParam(required = false) Integer id,
                                    @RequestParam String name,
                                    @RequestParam(required = false) Integer sortOrder,
                                    @RequestParam Map<String, String> params,
                                    Authentication authentication) {
        catalogAdministration.savePizzaCategory(id, name, sortOrder, params, actor(authentication));
        return "redirect:/admin#categories";
    }

    @PostMapping("/menu-categories/save")
    public String saveMenuCategory(@RequestParam(required = false) Integer id,
                                   @RequestParam String name,
                                   @RequestParam(required = false) Integer sortOrder,
                                   @RequestParam Map<String, String> params,
                                   Authentication authentication) {
        catalogAdministration.saveMenuCategory(id, name, sortOrder, params, actor(authentication));
        return "redirect:/admin#categories";
    }

    @PostMapping("/pizza-sizes/save")
    public String savePizzaSize(@RequestParam(required = false) Integer id,
                                @RequestParam Integer cm,
                                @RequestParam(required = false) Integer sortOrder,
                                @RequestParam Map<String, String> params,
                                Authentication authentication) {
        catalogAdministration.savePizzaSize(id, cm, sortOrder, params, actor(authentication));
        return "redirect:/admin#categories";
    }

    @PostMapping("/price-categories/save")
    public String savePriceCategory(@RequestParam(required = false) Integer id,
                                    @RequestParam String name,
                                    @RequestParam(required = false) Integer sortOrder,
                                    @RequestParam Map<String, String> params,
                                    Authentication authentication) {
        catalogAdministration.savePriceCategory(id, name, sortOrder, params, actor(authentication));
        return "redirect:/admin#categories";
    }

    @PostMapping("/ingredients/save")
    public String saveIngredient(@RequestParam(required = false) Integer id,
                                 @RequestParam String name,
                                 @RequestParam Integer priceCategoryId,
                                 @RequestParam Map<String, String> params,
                                 Authentication authentication) {
        catalogAdministration.saveIngredient(id, name, priceCategoryId, params, actor(authentication));
        return "redirect:/admin#ingredients";
    }

    @PostMapping("/modifier-groups/save")
    public String saveModifierGroup(@RequestParam(required = false) Integer id,
                                    @RequestParam String name,
                                    @RequestParam(required = false) Integer minSelect,
                                    @RequestParam(required = false) Integer maxSelect,
                                    @RequestParam Map<String, String> params,
                                    Authentication authentication) {
        catalogAdministration.saveModifierGroup(id, name, minSelect, maxSelect, params, actor(authentication));
        return "redirect:/admin#modifiers";
    }

    @PostMapping("/modifier-options/save")
    public String saveModifierOption(@RequestParam(required = false) Integer id,
                                     @RequestParam Integer groupId,
                                     @RequestParam String name,
                                     @RequestParam(required = false) String menuItemId,
                                     @RequestParam(required = false) BigDecimal additionalPrice,
                                     Authentication authentication) {
        catalogAdministration.saveModifierOption(id, groupId, name, menuItemId, additionalPrice, actor(authentication));
        return "redirect:/admin#modifiers";
    }

    private String actor(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
