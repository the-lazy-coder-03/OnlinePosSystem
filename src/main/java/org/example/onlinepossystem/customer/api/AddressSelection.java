package org.example.onlinepossystem.customer.api;

import java.math.BigDecimal;

public record AddressSelection(
        String googlePlaceId,
        String formattedAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String houseNumber,
        String street,
        String area,
        String city,
        String postalCode,
        String complexName,
        String province,
        String country
) {
    public boolean hasGoogleSelection() {
        return present(googlePlaceId) || present(formattedAddress) || latitude != null || longitude != null
                || present(province) || present(country);
    }

    public boolean isVerifiedGoogleAddress() {
        return present(googlePlaceId)
                && present(formattedAddress)
                && latitude != null
                && longitude != null
                && inRange(latitude, "-90", "90")
                && inRange(longitude, "-180", "180")
                && present(street)
                && (present(area) || present(city))
                && present(country);
    }

    public static AddressSelection empty() {
        return new AddressSelection(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean inRange(BigDecimal value, String min, String max) {
        return value.compareTo(new BigDecimal(min)) >= 0 && value.compareTo(new BigDecimal(max)) <= 0;
    }
}
