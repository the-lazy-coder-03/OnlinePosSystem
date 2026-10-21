package org.example.onlinepossystem.customer.dto;

import org.example.onlinepossystem.customer.api.AddressSelection;

public record CustomerProfileResponse(
        String firstName,
        String lastName,
        String email,
        String phone,
        String phone2,
        String preferredStore,
        String complexName,
        AddressSelection address
) {}
