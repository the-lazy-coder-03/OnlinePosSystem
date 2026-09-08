package org.example.onlinepossystem.customer.dto;

import org.example.onlinepossystem.customer.entity.Customer;

import java.util.List;

public record ProfilePageView(
        Customer customer,
        List<ProfileOrderView> recentOrders
) {
}
