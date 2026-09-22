package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.customer.api.AccountAccess;
import org.example.onlinepossystem.customer.api.CustomerRecordLookup;
import org.example.onlinepossystem.customer.api.PagedResult;
import org.example.onlinepossystem.ordering.api.AdminCustomerOrderView;
import org.example.onlinepossystem.ordering.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCustomerOrderService {
    private static final int PAGE_SIZE = 10;
    private final OrderRepository orders;
    private final CustomerRecordLookup customers;
    private final OrderResponseMapper mapper;
    private final OrderTotalCalculator totals;

    public AdminCustomerOrderService(OrderRepository orders, CustomerRecordLookup customers,
                                     OrderResponseMapper mapper, OrderTotalCalculator totals) {
        this.orders = orders;
        this.customers = customers;
        this.mapper = mapper;
        this.totals = totals;
    }

    @Transactional(readOnly = true)
    public PagedResult<AdminCustomerOrderView> history(Long customerId, int page, AccountAccess access) {
        if (!access.isAdmin()) throw new org.springframework.security.access.AccessDeniedException("Admin access required.");
        if (page < 0) throw new IllegalArgumentException("Page must be non-negative.");
        customers.requireCustomer(customerId);
        PageRequest paging = PageRequest.of(page, PAGE_SIZE);
        Page<AdminCustomerOrderView> result = (access.branchId() == null
                ? orders.findAllByCustomerIdOrderByCreatedAtDescIdDesc(customerId, paging)
                : orders.findAllByCustomerIdAndBranchIdOrderByCreatedAtDescIdDesc(
                        customerId, access.branchId(), paging))
                .map(order -> {
                    var dto = mapper.toDto(order);
                    return new AdminCustomerOrderView(dto, totals.total(dto));
                });
        return PagedResult.from(result);
    }
}
