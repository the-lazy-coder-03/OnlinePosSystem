package org.example.onlinepossystem.special.dto;

import java.math.BigDecimal;
import java.util.List;

public record SpecialQuoteResponse(Long specialId, String specialName, int quantity,
                                   BigDecimal basePrice, BigDecimal customizationTotal,
                                   BigDecimal addonTotal, BigDecimal lineTotal,
                                   List<Selection> selections) {
    public record Selection(String kind, Long ruleId, int index, String label, String productType,
                            Integer productId, String productName, Integer sizeCm, int quantity,
                            BigDecimal addonPrice, BigDecimal customizationCharge) {}
}
