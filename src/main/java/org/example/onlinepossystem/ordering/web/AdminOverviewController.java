package org.example.onlinepossystem.ordering.web;

import org.example.onlinepossystem.customer.api.AccountAccess;
import org.example.onlinepossystem.customer.api.AccountAccessReader;
import org.example.onlinepossystem.ordering.api.AdminOverview;
import org.example.onlinepossystem.ordering.service.AdminOverviewService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminOverviewController {
    private final AdminOverviewService adminOverviewService;
    private final AccountAccessReader accountAccessReader;

    public AdminOverviewController(AdminOverviewService adminOverviewService,
                                   AccountAccessReader accountAccessReader) {
        this.adminOverviewService = adminOverviewService;
        this.accountAccessReader = accountAccessReader;
    }

    @GetMapping("/api/admin/overview")
    public AdminOverview overview(
            @RequestParam(required = false, defaultValue = "today") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Integer branchId,
            Authentication authentication
    ) {
        AccountAccess access = accountAccessReader.findByUsername(authentication.getName());
        return adminOverviewService.getOverview(access, range, start, end, branchId);
    }
}
