package org.example.onlinepossystem.customer.api;

public record AccountAccessSummary(
        Long id,
        String name,
        String email,
        String phone,
        int accessLevel,
        String accessLabel
) {
}
