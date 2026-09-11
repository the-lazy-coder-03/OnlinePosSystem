package org.example.onlinepossystem.catalog.service;

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

    static String cleanText(String value) {
        return value == null ? null : value.trim();
    }

    static String actorName(String actor) {
        return actor == null || actor.isBlank() ? "unknown" : actor;
    }
}
