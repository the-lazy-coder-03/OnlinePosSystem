package org.example.onlinepossystem.customer.web;

import org.example.onlinepossystem.customer.api.AccountAccessAdministration;
import org.example.onlinepossystem.customer.api.AccountAccessSummary;
import org.example.onlinepossystem.customer.api.AdminAccountCreator;
import org.example.onlinepossystem.customer.service.CustomerRegistrationException;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final AdminAccountCreator adminAccountCreator;

    public CustomerAccessAdminController(AccountAccessAdministration accountAccessAdministration,
                                         AdminAccountCreator adminAccountCreator) {
        this.accountAccessAdministration = accountAccessAdministration;
        this.adminAccountCreator = adminAccountCreator;
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

    @PostMapping("/admin/accounts")
    public String createAdmin(@RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam String email,
                              @RequestParam String password,
                              @RequestParam int accessLevel) {
        try {
            adminAccountCreator.create(firstName, lastName, email, password, accessLevel);
            return "redirect:/admin?accountCreated#accounts";
        } catch (CustomerRegistrationException exception) {
            String error = exception.getReason() == CustomerRegistrationException.Reason.EMAIL ? "email" : "password";
            return "redirect:/admin?accountError=" + error + "#accounts";
        } catch (DataIntegrityViolationException exception) {
            return "redirect:/admin?accountError=email#accounts";
        } catch (IllegalArgumentException exception) {
            return "redirect:/admin?accountError=invalid#accounts";
        }
    }
}
