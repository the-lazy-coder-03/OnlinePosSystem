package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class OrderTotalCalculator {
    public BigDecimal total(OrderResponseDTO order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderResponseDTO.MenuItemDTO item : safeList(order.getMenuItems())) {
            int itemQuantity = quantity(item.getQty());
            total = total.add(money(item.getUnitPriceAtTime()).multiply(BigDecimal.valueOf(itemQuantity)));
            for (OrderResponseDTO.MenuItemExtraDTO extra : safeList(item.getExtras())) {
                total = total.add(money(extra.getUnitPriceAtTime())
                        .multiply(BigDecimal.valueOf(quantity(extra.getQty())))
                        .multiply(BigDecimal.valueOf(itemQuantity)));
            }
        }
        for (OrderResponseDTO.PizzaItemDTO item : safeList(order.getPizzaItems())) {
            int itemQuantity = quantity(item.getQty());
            total = total.add(money(item.getBasePriceAtTime()).multiply(BigDecimal.valueOf(itemQuantity)));
            total = total.add(money(item.getPizzaBaseOptionPriceAtTime()).multiply(BigDecimal.valueOf(itemQuantity)));
            for (OrderResponseDTO.PizzaItemExtraDTO extra : safeList(item.getExtras())) {
                total = total.add(money(extra.getUnitPriceAtTime())
                        .multiply(BigDecimal.valueOf(quantity(extra.getQty())))
                        .multiply(BigDecimal.valueOf(itemQuantity)));
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private int quantity(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
