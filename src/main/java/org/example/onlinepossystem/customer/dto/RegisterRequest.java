package org.example.onlinepossystem.customer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.onlinepossystem.customer.api.AddressSelection;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 512) String password,
        @NotBlank @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$") String phone,
        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,20}$") String phone2,
        @Size(max = 100) String preferredStore,
        @NotNull @Valid AddressSelection address
) {}
