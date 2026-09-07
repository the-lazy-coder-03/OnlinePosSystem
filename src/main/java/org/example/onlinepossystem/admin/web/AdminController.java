package org.example.onlinepossystem.admin.web;

import org.example.onlinepossystem.catalog.service.CatalogAdminService;
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
    private final CatalogAdminService catalogAdminService;

    public AdminController(CatalogAdminService catalogAdminService) {
        this.catalogAdminService = catalogAdminService;
    }

    @GetMapping
    public String adminDashboard(Model model) {
        catalogAdminService.populateDashboard(model);
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
        return catalogAdminService.savePizza(id, name, categoryId, description, sortOrder, ingredientIds, params, authentication);
    }

    @PostMapping("/pizzas/delete/{id}")
    public String deletePizza(@PathVariable Integer id, Authentication authentication) {
        return catalogAdminService.deletePizza(id, authentication);
    }

    @PostMapping("/pizzas/price")
    public String updatePizzaPrice(@RequestParam Integer branchId,
                                   @RequestParam Integer pizzaId,
                                   @RequestParam Integer pizzaSizeId,
                                   @RequestParam Double price,
                                   Authentication authentication) {
        return catalogAdminService.updatePizzaPrice(branchId, pizzaId, pizzaSizeId, price, authentication);
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
        return catalogAdminService.saveMenuItem(id, name, categoryId, description, sortOrder, modifierGroupIds, params, authentication);
    }

    @PostMapping("/menu-items/delete/{id}")
    public String deleteMenuItem(@PathVariable Integer id, Authentication authentication) {
        return catalogAdminService.deleteMenuItem(id, authentication);
    }

    @PostMapping("/menu-items/price")
    public String updateMenuItemPrice(@RequestParam Integer branchId,
                                      @RequestParam Integer menuItemId,
                                      @RequestParam Double price,
                                      Authentication authentication) {
        return catalogAdminService.updateMenuItemPrice(branchId, menuItemId, price, authentication);
    }

    @PostMapping("/toppings/price")
    public String updateToppingPrice(@RequestParam Integer branchId,
                                     @RequestParam Integer priceCategoryId,
                                     @RequestParam Integer pizzaSizeId,
                                     @RequestParam Double price,
                                     Authentication authentication) {
        return catalogAdminService.updateToppingPrice(branchId, priceCategoryId, pizzaSizeId, price, authentication);
    }

    @PostMapping("/pizza-categories/save")
    public String savePizzaCategory(@RequestParam(required = false) Integer id,
                                    @RequestParam String name,
                                    @RequestParam(required = false) Integer sortOrder,
                                    @RequestParam Map<String, String> params,
                                    Authentication authentication) {
        return catalogAdminService.savePizzaCategory(id, name, sortOrder, params, authentication);
    }

    @PostMapping("/menu-categories/save")
    public String saveMenuCategory(@RequestParam(required = false) Integer id,
                                   @RequestParam String name,
                                   @RequestParam(required = false) Integer sortOrder,
                                   @RequestParam Map<String, String> params,
                                   Authentication authentication) {
        return catalogAdminService.saveMenuCategory(id, name, sortOrder, params, authentication);
    }

    @PostMapping("/pizza-sizes/save")
    public String savePizzaSize(@RequestParam(required = false) Integer id,
                                @RequestParam Integer cm,
                                @RequestParam(required = false) Integer sortOrder,
                                @RequestParam Map<String, String> params,
                                Authentication authentication) {
        return catalogAdminService.savePizzaSize(id, cm, sortOrder, params, authentication);
    }

    @PostMapping("/price-categories/save")
    public String savePriceCategory(@RequestParam(required = false) Integer id,
                                    @RequestParam String name,
                                    @RequestParam(required = false) Integer sortOrder,
                                    @RequestParam Map<String, String> params,
                                    Authentication authentication) {
        return catalogAdminService.savePriceCategory(id, name, sortOrder, params, authentication);
    }

    @PostMapping("/ingredients/save")
    public String saveIngredient(@RequestParam(required = false) Integer id,
                                 @RequestParam String name,
                                 @RequestParam Integer priceCategoryId,
                                 @RequestParam Map<String, String> params,
                                 Authentication authentication) {
        return catalogAdminService.saveIngredient(id, name, priceCategoryId, params, authentication);
    }

    @PostMapping("/modifier-groups/save")
    public String saveModifierGroup(@RequestParam(required = false) Integer id,
                                    @RequestParam String name,
                                    @RequestParam(required = false) Integer minSelect,
                                    @RequestParam(required = false) Integer maxSelect,
                                    @RequestParam Map<String, String> params,
                                    Authentication authentication) {
        return catalogAdminService.saveModifierGroup(id, name, minSelect, maxSelect, params, authentication);
    }

    @PostMapping("/modifier-options/save")
    public String saveModifierOption(@RequestParam(required = false) Integer id,
                                     @RequestParam Integer groupId,
                                     @RequestParam String name,
                                     @RequestParam(required = false) String menuItemId,
                                     @RequestParam(required = false) BigDecimal additionalPrice,
                                     Authentication authentication) {
        return catalogAdminService.saveModifierOption(id, groupId, name, menuItemId, additionalPrice, authentication);
    }
}
