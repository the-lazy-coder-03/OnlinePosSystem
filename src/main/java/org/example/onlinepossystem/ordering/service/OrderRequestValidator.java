package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class OrderRequestValidator {
    private static final String GATE_ACCESS_PATTERN = "[A-Za-z0-9 #*]*";

    public void validate(OrderRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Order request is required.");
        }
        validateGateAccessCode(request.getGateAccessCode());
        validateDeliveryAddress(request);
        boolean hasItems = request.getItems() != null && !request.getItems().isEmpty();
        boolean hasSpecials = request.getSpecialItems() != null && !request.getSpecialItems().isEmpty();
        if (!hasItems && !hasSpecials) {
            throw new IllegalArgumentException("Order must include at least one item.");
        }
        if (!hasItems) {
            return;
        }
        for (OrderRequestDTO.OrderItemRequestDTO item : request.getItems()) {
            if (item == null) {
                throw new IllegalArgumentException("Order items cannot be null.");
            }
            boolean hasMenuItem = item.getMenuItemId() != null;
            boolean hasPizza = item.getPizzaId() != null;
            if (hasMenuItem == hasPizza) {
                throw new IllegalArgumentException("Each item must include exactly one of menuItemId or pizzaId.");
            }
        }
    }

    private void validateDeliveryAddress(OrderRequestDTO request) {
        if (!"delivery".equalsIgnoreCase(request.getOrderType())) {
            return;
        }
        if (request.getDeliveryAddress() == null || !request.getDeliveryAddress().isVerifiedGoogleAddress()) {
            throw new IllegalArgumentException("Delivery orders require a verified Google address selection.");
        }
        validateDeliveryAddressLengths(request.getDeliveryAddress());
    }

    private void validateDeliveryAddressLengths(org.example.onlinepossystem.customer.api.AddressSelection address) {
        requireMax(address.googlePlaceId(), 255, "Google place ID");
        requireMax(address.formattedAddress(), 512, "Formatted address");
        requireMax(address.houseNumber(), 100, "House number");
        requireMax(address.street(), 255, "Street");
        requireMax(address.area(), 120, "Area");
        requireMax(address.city(), 120, "City");
        requireMax(address.postalCode(), 20, "Postal code");
        requireMax(address.complexName(), 120, "Complex name");
        requireMax(address.province(), 120, "Province");
        requireMax(address.country(), 120, "Country");
    }

    private void requireMax(String value, int max, String label) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(label + " must be " + max + " characters or fewer.");
        }
    }

    private void validateGateAccessCode(String value) {
        if (value == null) return;
        if (value.length() > 64) {
            throw new IllegalArgumentException("Gate access code must be 64 characters or fewer.");
        }
        if (!value.matches(GATE_ACCESS_PATTERN)) {
            throw new IllegalArgumentException("Gate access code may contain only letters, numbers, spaces, # and *.");
        }
    }
}
