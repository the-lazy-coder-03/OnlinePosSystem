package org.example.onlinepossystem.shared.web;

import org.example.onlinepossystem.customer.api.CustomerAccount;
import org.example.onlinepossystem.customer.api.AccountAccessReader;
import org.example.onlinepossystem.customer.api.CustomerAccountReader;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;
import java.util.Optional;

@Controller
public class MainController {

    private final CustomerAccountReader customerAccountReader;
    private final AccountAccessReader accountAccessReader;

    public MainController(CustomerAccountReader customerAccountReader, AccountAccessReader accountAccessReader) {
        this.customerAccountReader = customerAccountReader;
        this.accountAccessReader = accountAccessReader;
    }

    // ====== Public Pages ======

    @GetMapping({"/", "/home"})
    public String mainPage(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            customerAccountReader.findByEmail(email).ifPresent(customer -> {
                model.addAttribute("customerName", customer.firstName());
                model.addAttribute("user", customer);
            });
        }
        return "index";
    }

    @GetMapping("/menu")
    public String menuPage(Model model) {
        model.addAttribute("menuOnly", true);
        return "PlaceOrder";
    }

    @GetMapping("/menu/kenridge")
    public String kenridgeMenu(Model model) {
        model.addAttribute("menuOnly", true);
        model.addAttribute("branchId", 1);
        model.addAttribute("branchName", "Kenridge");
        return "PlaceOrder";
    }

    @GetMapping("/menu/uitzicht")
    public String uitzichtMenu(Model model) {
        model.addAttribute("menuOnly", true);
        model.addAttribute("branchId", 2);
        model.addAttribute("branchName", "Uitzicht");
        return "PlaceOrder";
    }

    @GetMapping("/order")
    public String orderPage(
            @RequestParam(value = "branchId", required = false) Integer branchId,
            Model model,
            Authentication authentication
    ) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            addCustomerDetails(model, email);
        }

        model.addAttribute("branchId", branchId != null ? branchId : 1);
        return "PlaceOrder";
    }

    @GetMapping("/checkout")
    public String checkoutPage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        addCustomerDetails(model, authentication.getName());
        return "checkout";
    }

    private void addCustomerDetails(Model model, String email) {
        customerAccountReader.findByEmail(email).ifPresent(customer -> {
            String fullName = (Optional.ofNullable(customer.firstName()).orElse("") + " "
                    + Optional.ofNullable(customer.lastName()).orElse("")).trim();
            model.addAttribute("customerName", fullName);
            model.addAttribute("user", customer);
            resolvePreferredBranch(customer).ifPresent(branch -> {
                model.addAttribute("preferredBranchId", branch.id());
                model.addAttribute("preferredBranchName", branch.name());
            });
        });
    }

    private Optional<PreferredBranch> resolvePreferredBranch(CustomerAccount customer) {
        String preferredStore = normalizeBranchName(customer.preferredStore());
        if (preferredStore.isEmpty()) {
            return Optional.empty();
        }

        return switch (preferredStore) {
            case "kenridge" -> Optional.of(new PreferredBranch(1, "Kenridge"));
            case "uitzicht" -> Optional.of(new PreferredBranch(2, "Uitzicht"));
            default -> Optional.empty();
        };
    }

    private String normalizeBranchName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceFirst("\\s+branch$", "")
                .replaceAll("\\s+", " ");
    }

    private record PreferredBranch(Integer id, String name) {
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/admin/login")
    public String adminLoginPage() {
        return "admin-login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping({"/input-orders", "/orders", "/InputOrders", "/InputOrders.html"})
    public String inputOrdersPage(Model model, Authentication authentication) {
        var access = accountAccessReader.findByUsername(authentication.getName());
        if (!access.isAdmin()) throw new org.springframework.security.access.AccessDeniedException("Admin access is required.");
        model.addAttribute("adminMode", true);
        model.addAttribute("adminBranchId", access.branchId());
        model.addAttribute("adminBranchName", access.branchId() == null ? null :
                access.branchId() == 1 ? "Kenridge" : "Uitzicht");
        return "InputOrders";
    }
}
