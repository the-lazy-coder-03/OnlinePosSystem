package org.example.onlinepossystem.ordering.web;

import org.example.onlinepossystem.customer.api.AccountAccessReader;
import org.example.onlinepossystem.customer.api.PagedResult;
import org.example.onlinepossystem.ordering.api.AdminCustomerOrderView;
import org.example.onlinepossystem.ordering.service.AdminCustomerOrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/customers/{customerId}/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerOrderController {
    private final AdminCustomerOrderService service;
    private final AccountAccessReader accessReader;

    public AdminCustomerOrderController(AdminCustomerOrderService service, AccountAccessReader accessReader) {
        this.service = service;
        this.accessReader = accessReader;
    }

    @GetMapping
    public PagedResult<AdminCustomerOrderView> history(@PathVariable Long customerId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        Authentication authentication) {
        return service.history(customerId, page, accessReader.findByUsername(authentication.getName()));
    }
}
