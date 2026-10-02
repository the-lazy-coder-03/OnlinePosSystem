package org.example.onlinepossystem.ordering.api;

import java.math.BigDecimal;

public record OrderPriceBreakdown(
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal discountTotal,
        BigDecimal total,
        boolean hasDiscount
) {
}
