package org.example.onlinepossystem.customer.api;

import java.time.LocalDateTime;

public record AdminCustomerView(
        Long id, String firstName, String lastName, String email, String phone1, String phone2,
        String houseNumber, String street, String area, String complexName, String city,
        String postalCode, String preferredStore, LocalDateTime lastOrderedAt,
        int accessLevel, String accessLabel
) {}
