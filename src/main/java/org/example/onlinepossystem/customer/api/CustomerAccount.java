package org.example.onlinepossystem.customer.api;

import java.time.LocalDateTime;

public record CustomerAccount(
        Long id,
        String phone1,
        String phone2,
        String email,
        String houseNumber,
        String street,
        String area,
        String complexName,
        LocalDateTime lastOrderedAt,
        String preferredStore,
        String city,
        String postalCode,
        String firstName,
        String lastName,
        String role
) {
}
