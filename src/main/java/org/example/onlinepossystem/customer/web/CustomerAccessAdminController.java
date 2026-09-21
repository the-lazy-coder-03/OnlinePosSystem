package org.example.onlinepossystem.customer.web;

import org.example.onlinepossystem.customer.api.AccountAccessAdministration;
import org.example.onlinepossystem.customer.api.AccountAccessSummary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class CustomerAccessAdminController {
    private final AccountAccessAdministration accountAccessAdministration;

    public CustomerAccessAdminController(AccountAccessAdministration accountAccessAdministration) {
        this.accountAccessAdministration = accountAccessAdministration;
    }

    @GetMapping("/api/admin/accounts")
    @ResponseBody
    public List<AccountAccessSummary> searchAccounts(@RequestParam(required = false) String query) {
        return accountAccessAdministration.search(query);
    }

    @PostMapping("/admin/accounts/{customerId}/access-level")
    public String assignAccessLevel(@PathVariable Long customerId,
                                    @RequestParam int accessLevel) {
        accountAccessAdministration.assignAccessLevel(customerId, accessLevel);
        return "redirect:/admin#accounts";
    }
}
