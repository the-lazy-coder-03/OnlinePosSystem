package org.example.onlinepossystem.profile.dto;

import org.example.onlinepossystem.customer.api.CustomerAccount;

import java.util.List;

public record ProfilePageView(CustomerAccount customer, List<ProfileOrderView> recentOrders) {
}
