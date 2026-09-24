package org.example.onlinepossystem.profile.service;

import org.example.onlinepossystem.customer.api.CustomerAccountReader;
import org.example.onlinepossystem.ordering.api.CustomerOrderHistoryReader;
import org.example.onlinepossystem.ordering.api.CustomerOrderSummary;
import org.example.onlinepossystem.profile.dto.ProfileOrderView;
import org.example.onlinepossystem.profile.dto.ProfilePageView;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class ProfilePageService {
    private static final int RECENT_ORDER_LIMIT = 10;
    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CustomerAccountReader customerAccountReader;
    private final CustomerOrderHistoryReader customerOrderHistoryReader;

    public ProfilePageService(
            CustomerAccountReader customerAccountReader,
            CustomerOrderHistoryReader customerOrderHistoryReader
    ) {
        this.customerAccountReader = customerAccountReader;
        this.customerOrderHistoryReader = customerOrderHistoryReader;
    }

    public Optional<ProfilePageView> getProfilePage(String customerEmail) {
        return customerAccountReader.findByEmail(customerEmail)
                .map(customer -> new ProfilePageView(
                        customer,
                        customerOrderHistoryReader.getRecentOrdersForCustomer(customerEmail, RECENT_ORDER_LIMIT)
                                .stream()
                                .map(this::toProfileOrder)
                                .toList()
                ));
    }

    public Optional<ProfilePageView> getProfilePageForCustomerId(Long customerId) {
        return customerAccountReader.findById(customerId)
                .map(customer -> new ProfilePageView(customer,
                        customerOrderHistoryReader.getRecentOrdersForCustomerId(customerId, RECENT_ORDER_LIMIT)
                                .stream().map(this::toProfileOrder).toList()));
    }

    private ProfileOrderView toProfileOrder(CustomerOrderSummary order) {
        return new ProfileOrderView(
                order.id(),
                order.createdAt() == null ? "" : order.createdAt().format(ORDER_DATE_FORMAT),
                display(order.branchName(), "Unknown branch"),
                displayStatus(order.status()),
                display(order.orderType(), "N/A"),
                order.itemLines(),
                formatMoney(order.total())
        );
    }

    private String formatMoney(BigDecimal value) {
        return "R" + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String display(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String displayStatus(String status) {
        String value = display(status, "Pending");
        return "created".equalsIgnoreCase(value) ? "Pending" : value;
    }
}
