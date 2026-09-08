package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.api.CustomerAccountReader;
import org.example.onlinepossystem.customer.dto.ProfileOrderView;
import org.example.onlinepossystem.customer.dto.ProfilePageView;
import org.example.onlinepossystem.ordering.api.CustomerOrderHistoryReader;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomerProfileService {
    private static final int RECENT_ORDER_LIMIT = 10;
    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CustomerAccountReader customerAccountReader;
    private final CustomerOrderHistoryReader customerOrderHistoryReader;

    public CustomerProfileService(CustomerAccountReader customerAccountReader,
                                  CustomerOrderHistoryReader customerOrderHistoryReader) {
        this.customerAccountReader = customerAccountReader;
        this.customerOrderHistoryReader = customerOrderHistoryReader;
    }

    public Optional<ProfilePageView> getProfilePage(String customerEmail) {
        return customerAccountReader.findByEmail(customerEmail)
                .map(customer -> new ProfilePageView(
                        customer,
                        customerOrderHistoryReader.getRecentOrdersForCustomer(customerEmail, RECENT_ORDER_LIMIT)
                                .stream()
                                .map(this::toProfileOrder)
                                .toList()
                ));
    }

    private ProfileOrderView toProfileOrder(OrderResponseDTO order) {
        return new ProfileOrderView(
                order.getId(),
                order.getCreatedAt() == null ? "" : order.getCreatedAt().format(ORDER_DATE_FORMAT),
                display(order.getBranchName(), "Unknown branch"),
                displayStatus(order.getStatus()),
                display(order.getOrderType(), "N/A"),
                itemLines(order),
                formatMoney(orderTotal(order))
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
        if (extras == null || extras.isBlank()) {
            return line;
        }
        return line + " - Extras: " + extras;
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
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value);
    }

    private String formatMoney(BigDecimal value) {
        return "R" + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private int quantity(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private String display(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String displayStatus(String status) {
        String value = display(status, "Pending");
        return "created".equalsIgnoreCase(value) ? "Pending" : value;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
