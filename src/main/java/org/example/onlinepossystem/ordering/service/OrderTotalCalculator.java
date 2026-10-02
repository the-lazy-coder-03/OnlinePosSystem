package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.api.OrderPriceBreakdown;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class OrderTotalCalculator {
    private static final int MONEY_SCALE = 2;

    public BigDecimal total(OrderResponseDTO order) {
        return breakdown(order).total();
    }

    public OrderPriceBreakdown breakdown(OrderResponseDTO order) {
        BigDecimal subtotal = subtotal(order);
        BigDecimal deliveryFee = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal discountTotal = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(deliveryFee).subtract(discountTotal).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new OrderPriceBreakdown(subtotal, deliveryFee, discountTotal, total,
                discountTotal.compareTo(BigDecimal.ZERO) > 0);
    }

    public BigDecimal subtotal(OrderResponseDTO order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderResponseDTO.SpecialItemDTO item : safeList(order.getSpecialItems())) {
            total = total.add(specialLineTotal(item));
        }
        for (OrderResponseDTO.MenuItemDTO item : safeList(order.getMenuItems())) {
            total = total.add(menuItemLineTotal(item));
        }
        for (OrderResponseDTO.PizzaItemDTO item : safeList(order.getPizzaItems())) {
            total = total.add(pizzaItemLineTotal(item));
        }
        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal menuItemUnitTotal(OrderResponseDTO.MenuItemDTO item) {
        BigDecimal unit = money(item.getUnitPriceAtTime());
        for (OrderResponseDTO.MenuItemExtraDTO extra : safeList(item.getExtras())) {
            unit = unit.add(money(extra.getUnitPriceAtTime()).multiply(BigDecimal.valueOf(quantity(extra.getQty()))));
        }
        return unit.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal menuItemLineTotal(OrderResponseDTO.MenuItemDTO item) {
        return menuItemUnitTotal(item).multiply(BigDecimal.valueOf(quantity(item.getQty())))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal pizzaItemUnitTotal(OrderResponseDTO.PizzaItemDTO item) {
        BigDecimal unit = money(item.getBasePriceAtTime()).add(money(item.getPizzaBaseOptionPriceAtTime()));
        for (OrderResponseDTO.PizzaItemExtraDTO extra : safeList(item.getExtras())) {
            unit = unit.add(money(extra.getUnitPriceAtTime()).multiply(BigDecimal.valueOf(quantity(extra.getQty()))));
        }
        return unit.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal pizzaItemLineTotal(OrderResponseDTO.PizzaItemDTO item) {
        return pizzaItemUnitTotal(item).multiply(BigDecimal.valueOf(quantity(item.getQty())))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal specialUnitTotal(OrderResponseDTO.SpecialItemDTO item) {
        BigDecimal line = specialLineTotal(item);
        return line.divide(BigDecimal.valueOf(quantity(item.getQuantity())), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal specialLineTotal(OrderResponseDTO.SpecialItemDTO item) {
        return money(item.getFinalLineTotalAtTime()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
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
