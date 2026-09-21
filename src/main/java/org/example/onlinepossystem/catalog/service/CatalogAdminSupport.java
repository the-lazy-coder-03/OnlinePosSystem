package org.example.onlinepossystem.catalog.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

final class CatalogAdminSupport {
    private CatalogAdminSupport() {
    }

    static <T> Integer nextId(List<T> items, Function<T, Integer> idExtractor) {
        return items.stream()
                .map(idExtractor)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    static Optional<Double> parsePrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        double price = Double.parseDouble(raw);
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        return Optional.of(price);
    }

    static double requiredPrice(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("A price is required for every branch and selected size.");
        }
        try {
            BigDecimal price = new BigDecimal(raw.trim());
            if (price.signum() < 0 || price.scale() > 2 || price.compareTo(new BigDecimal("99999999.99")) > 0) {
                throw new IllegalArgumentException("Prices must be between 0.00 and 99999999.99 with at most two decimal places.");
            }
            return price.doubleValue();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a valid numeric price.", ex);
        }
    }

    static String cleanText(String value) {
        return value == null ? null : value.trim();
    }

    static String actorName(String actor) {
        return actor == null || actor.isBlank() ? "unknown" : actor;
    }
}
