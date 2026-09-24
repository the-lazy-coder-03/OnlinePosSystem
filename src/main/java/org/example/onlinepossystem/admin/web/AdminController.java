package org.example.onlinepossystem.admin.web;

import org.example.onlinepossystem.catalog.api.CatalogAdministration;
import org.example.onlinepossystem.customer.api.AccountAccess;
import org.example.onlinepossystem.customer.api.AccountAccessReader;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final AccountAccessReader accountAccessReader;

    public AdminController(CatalogAdministration catalogAdministration,
                           AccountAccessReader accountAccessReader) {
        this.catalogAdministration = catalogAdministration;
        this.accountAccessReader = accountAccessReader;
    }

    @GetMapping
    public String adminDashboard(Model model, Authentication authentication) {
        AccountAccess access = access(authentication);
        model.addAllAttributes(catalogAdministration.getDashboardAttributes(access.branchId()));
        model.addAttribute("adminLevel", access.level());
        model.addAttribute("adminBranchId", access.branchId());
        model.addAttribute("adminBranchName", branchName(access.branchId()));
        model.addAttribute("superAdmin", access.isSuperAdmin());
        return "admin";
    }

    @GetMapping("/orders")
    public String adminOrdersPage() {
        return "redirect:/admin#orders";
    }

    @PostMapping("/pizzas/save")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String savePizza(@RequestParam(required = false) Integer id,
                            @RequestParam String name,
                            @RequestParam Integer categoryId,
                            @RequestParam(required = false) String description,
                            @RequestParam(required = false) List<Integer> ingredientIds,
                            @RequestParam Map<String, String> params,
                            Authentication authentication) {
        catalogAdministration.savePizza(id, name, categoryId, description, ingredientIds, params, actor(authentication));
        return "redirect:/admin#items";
    }

    @PostMapping("/pizzas/delete/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String deletePizza(@PathVariable Integer id, Authentication authentication) {
        catalogAdministration.deletePizza(id, actor(authentication));
        return "redirect:/admin#items";
    }

    @PostMapping("/pizzas/category-price")
    public String updatePizzaCategoryPrice(@RequestParam Integer branchId,
            @RequestParam Integer categoryId, @RequestParam Integer pizzaSizeId,
            @RequestParam String price, Authentication authentication,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        requireBranch(authentication, branchId);
        int count = catalogAdministration.updatePizzaCategoryPrice(branchId, categoryId, pizzaSizeId, price, actor(authentication));
        redirect.addFlashAttribute("pricingMessage", count == 0 ? "No matching prices found" : count + " prices updated.");
        return "redirect:/admin#pricing";
    }

    @PostMapping("/menu-items/category-price")
    public String updateMenuItemCategoryPrice(@RequestParam Integer branchId,
            @RequestParam Integer categoryId,
            @RequestParam String price, Authentication authentication,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        requireBranch(authentication, branchId);
        int count = catalogAdministration.updateMenuItemCategoryPrice(branchId, categoryId, price, actor(authentication));
        redirect.addFlashAttribute("pricingMessage", count == 0 ? "No matching prices found" : count + " prices updated.");
        return "redirect:/admin#pricing";
    }

    @PostMapping("/pizzas/price")
    public String updatePizzaPrice(@RequestParam Integer branchId,
                                   @RequestParam Integer pizzaId,
                                   @RequestParam Integer pizzaSizeId,
                                   @RequestParam Double price,
                                   Authentication authentication) {
        requireBranch(authentication, branchId);
        catalogAdministration.updatePizzaPrice(branchId, pizzaId, pizzaSizeId, price, actor(authentication));
        return "redirect:/admin#pricing";
    }

    @PostMapping("/menu-items/save")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String saveMenuItem(@RequestParam(required = false) Integer id,
                               @RequestParam String name,
                               @RequestParam Integer categoryId,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) List<Integer> modifierGroupIds,
                               @RequestParam Map<String, String> params,
                               Authentication authentication) {
        catalogAdministration.saveMenuItem(id, name, categoryId, description, modifierGroupIds, params, actor(authentication));
        return "redirect:/admin#items";
    }

    @PostMapping("/menu-items/delete/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String deleteMenuItem(@PathVariable Integer id, Authentication authentication) {
        catalogAdministration.deleteMenuItem(id, actor(authentication));
        return "redirect:/admin#items";
    }

    @PostMapping("/menu-items/price")
    public String updateMenuItemPrice(@RequestParam Integer branchId,
                                      @RequestParam Integer menuItemId,
                                      @RequestParam Double price,
                                      Authentication authentication) {
        requireBranch(authentication, branchId);
        catalogAdministration.updateMenuItemPrice(branchId, menuItemId, price, actor(authentication));
        return "redirect:/admin#pricing";
    }

    @PostMapping("/toppings/price")
    public String updateToppingPrice(@RequestParam Integer branchId,
                                     @RequestParam Integer priceCategoryId,
                                     @RequestParam Integer pizzaSizeId,
                                     @RequestParam Double price,
                                     Authentication authentication) {
        requireBranch(authentication, branchId);
        catalogAdministration.updateToppingPrice(branchId, priceCategoryId, pizzaSizeId, price, actor(authentication));
        return "redirect:/admin#pricing";
    }

    @PostMapping("/pizza-categories/save")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String savePizzaCategory(@RequestParam(required = false) Integer id,
                                    @RequestParam String name,
                                    @RequestParam Map<String, String> params,
                                    Authentication authentication) {
        catalogAdministration.savePizzaCategory(id, name, params, actor(authentication));
        return "redirect:/admin#categories";
    }

    @PostMapping("/menu-categories/save")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String saveMenuCategory(@RequestParam(required = false) Integer id,
                                   @RequestParam String name,
                                   @RequestParam Map<String, String> params,
                                   Authentication authentication) {
        catalogAdministration.saveMenuCategory(id, name, params, actor(authentication));
        return "redirect:/admin#categories";
    }

    @PostMapping("/pizza-sizes/save")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String savePizzaSize(@RequestParam(required = false) Integer id,
                                @RequestParam Integer cm,
                                @RequestParam Map<String, String> params,
                                Authentication authentication) {
        catalogAdministration.savePizzaSize(id, cm, params, actor(authentication));
        return "redirect:/admin#categories";
    }

    @PostMapping("/price-categories/save")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String savePriceCategory(@RequestParam(required = false) Integer id,
                                    @RequestParam String name,
                                    @RequestParam Map<String, String> params,
                                    Authentication authentication) {
        catalogAdministration.savePriceCategory(id, name, params, actor(authentication));
        return "redirect:/admin#categories";
    }

    @PostMapping("/ingredients/save")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String saveIngredient(@RequestParam(required = false) Integer id,
                                 @RequestParam String name,
                                 @RequestParam Integer priceCategoryId,
                                 @RequestParam Map<String, String> params,
                                 Authentication authentication) {
        catalogAdministration.saveIngredient(id, name, priceCategoryId, params, actor(authentication));
        return "redirect:/admin#ingredients";
    }

    @PostMapping("/modifier-groups/save")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
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
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String saveModifierOption(@RequestParam(required = false) Integer id,
                                     @RequestParam Integer groupId,
                                     @RequestParam String name,
                                     @RequestParam(required = false) String menuItemId,
                                     @RequestParam(required = false) BigDecimal additionalPrice,
                                     Authentication authentication) {
        catalogAdministration.saveModifierOption(id, groupId, name, menuItemId, additionalPrice, actor(authentication));
        return "redirect:/admin#modifiers";
    }

    @org.springframework.web.bind.annotation.ExceptionHandler({
            IllegalArgumentException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public String invalidPriceInput(Exception exception, Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("message", exception instanceof IllegalArgumentException
                ? exception.getMessage() : "Select a valid branch, category and size.");
        return "error";
    }

    private String actor(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }

    private AccountAccess access(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Admin authentication is required.");
        }
        AccountAccess access = accountAccessReader.findByUsername(authentication.getName());
        if (!access.isAdmin()) {
            throw new AccessDeniedException("Admin access is required.");
        }
        return access;
    }

    private void requireBranch(Authentication authentication, Integer branchId) {
        if (!access(authentication).canAccessBranch(branchId)) {
            throw new AccessDeniedException("This admin account cannot manage the requested branch.");
        }
    }

    private String branchName(Integer branchId) {
        if (branchId == null) {
            return "All Branches";
        }
        return branchId == 1 ? "Kenridge" : "Uitzicht";
    }
}
