package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.api.CustomerOrderSummary;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomerOrderSummaryMapper {
    public CustomerOrderSummary toSummary(OrderResponseDTO order) {
        return new CustomerOrderSummary(
                order.getId(),
                order.getCreatedAt(),
                order.getBranchName(),
                order.getStatus(),
                order.getOrderType(),
                itemLines(order),
                orderTotal(order)
        );
    }

    private List<String> itemLines(OrderResponseDTO order) {
        List<String> lines = new ArrayList<>();
        for (OrderResponseDTO.MenuItemDTO item : safeList(order.getMenuItems())) {
            String line = quantity(item.getQty()) + " x " + display(item.getMenuItemName(), "Menu item");
            lines.add(withExtras(line, menuExtras(item)));
        }
        for (OrderResponseDTO.PizzaItemDTO item : safeList(order.getPizzaItems())) {
            String name = display(item.getPizzaName(), "Pizza");
            if (item.getPizzaSizeCm() != null) {
                name += " (" + item.getPizzaSizeCm() + "cm)";
            }
            lines.add(withExtras(quantity(item.getQty()) + " x " + name, pizzaExtras(item)));
        }
        return lines;
    }

    private String menuExtras(OrderResponseDTO.MenuItemDTO item) {
        return safeList(item.getExtras()).stream()
                .map(extra -> quantity(extra.getQty()) + " x " + display(extra.getName(), "Extra"))
                .collect(Collectors.joining(", "));
    }

    private String pizzaExtras(OrderResponseDTO.PizzaItemDTO item) {
        return safeList(item.getExtras()).stream()
                .map(extra -> quantity(extra.getQty()) + " x " + display(extra.getIngredientName(), "Extra"))
                .collect(Collectors.joining(", "));
    }

    private String withExtras(String line, String extras) {
        return extras == null || extras.isBlank() ? line : line + " - Extras: " + extras;
    }

    private BigDecimal orderTotal(OrderResponseDTO order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderResponseDTO.MenuItemDTO item : safeList(order.getMenuItems())) {
            total = total.add(money(item.getUnitPriceAtTime()).multiply(BigDecimal.valueOf(quantity(item.getQty()))));
            for (OrderResponseDTO.MenuItemExtraDTO extra : safeList(item.getExtras())) {
                total = total.add(money(extra.getUnitPriceAtTime()).multiply(BigDecimal.valueOf(quantity(extra.getQty()))));
            }
        }
        for (OrderResponseDTO.PizzaItemDTO item : safeList(order.getPizzaItems())) {
            total = total.add(money(item.getBasePriceAtTime()).multiply(BigDecimal.valueOf(quantity(item.getQty()))));
            for (OrderResponseDTO.PizzaItemExtraDTO extra : safeList(item.getExtras())) {
                total = total.add(money(extra.getUnitPriceAtTime()).multiply(BigDecimal.valueOf(quantity(extra.getQty()))));
            }
        }
        return total;
    }

    private BigDecimal money(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private int quantity(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private String display(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
