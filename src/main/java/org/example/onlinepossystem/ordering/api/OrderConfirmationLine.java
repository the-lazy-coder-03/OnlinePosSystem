package org.example.onlinepossystem.ordering.api;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmationLine(
        String category,
        String name,
        int quantity,
        String size,
        List<String> details,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
