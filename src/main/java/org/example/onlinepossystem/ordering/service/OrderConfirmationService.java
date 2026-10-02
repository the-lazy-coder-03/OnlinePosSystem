package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.customer.api.CustomerAccount;
import org.example.onlinepossystem.customer.api.CustomerAccountReader;
import org.example.onlinepossystem.ordering.api.OrderConfirmationLine;
import org.example.onlinepossystem.ordering.api.OrderConfirmationView;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderConfirmationService {
    private final OrderRepository orderRepository;
    private final CustomerAccountReader customerAccountReader;
    private final OrderResponseMapper orderResponseMapper;
    private final OrderTotalCalculator totals;
    private final String googleMapsApiKey;

    public OrderConfirmationService(OrderRepository orderRepository,
                                    CustomerAccountReader customerAccountReader,
                                    OrderResponseMapper orderResponseMapper,
                                    OrderTotalCalculator totals,
                                    @Value("${GOOGLE_MAPS_API_KEY:}") String googleMapsApiKey) {
        this.orderRepository = orderRepository;
        this.customerAccountReader = customerAccountReader;
        this.orderResponseMapper = orderResponseMapper;
        this.totals = totals;
        this.googleMapsApiKey = googleMapsApiKey == null ? "" : googleMapsApiKey.trim();
    }

    @Transactional(readOnly = true)
    public Optional<OrderConfirmationView> findForCustomer(Long orderId, String customerEmail) {
        if (orderId == null || customerEmail == null || customerEmail.isBlank()) {
            return Optional.empty();
        }
        return customerAccountReader.findByEmail(customerEmail)
                .map(CustomerAccount::id)
                .flatMap(customerId -> orderRepository.findByIdAndCustomerId(orderId, customerId))
                .map(orderResponseMapper::toDto)
                .map(this::toView);
    }

    private OrderConfirmationView toView(OrderResponseDTO order) {
        boolean delivery = isDelivery(order);
        String displayAddress = delivery ? displayAddress(order) : "";
        String mapUrl = delivery ? staticMapUrl(order) : "";
        String branchName = safeText(order.getBranchName(), "your selected branch");
        return new OrderConfirmationView(
                order,
                "#" + order.getId(),
                delivery,
                delivery ? "Delivery" : "Collection",
                "Your order has been received by Pete's Pizzas " + branchName + ".",
                displayAddress,
                !mapUrl.isBlank(),
                mapUrl,
                displayAddress.isBlank() ? "Delivery location map" : "Delivery location map for " + displayAddress,
                orderLines(order),
                totals.breakdown(order)
        );
    }

    private List<OrderConfirmationLine> orderLines(OrderResponseDTO order) {
        List<OrderConfirmationLine> lines = new ArrayList<>();
        for (OrderResponseDTO.MenuItemDTO item : safeList(order.getMenuItems())) {
            lines.add(new OrderConfirmationLine(
                    "Menu item",
                    safeText(item.getMenuItemName(), "Menu item"),
                    quantity(item.getQty()),
                    "",
                    menuDetails(item),
                    totals.menuItemUnitTotal(item),
                    totals.menuItemLineTotal(item)
            ));
        }
        for (OrderResponseDTO.PizzaItemDTO item : safeList(order.getPizzaItems())) {
            lines.add(new OrderConfirmationLine(
                    "Pizza",
                    safeText(item.getPizzaName(), "Pizza"),
                    quantity(item.getQty()),
                    item.getPizzaSizeCm() == null ? "" : item.getPizzaSizeCm() + " cm",
                    pizzaDetails(item),
                    totals.pizzaItemUnitTotal(item),
                    totals.pizzaItemLineTotal(item)
            ));
        }
        for (OrderResponseDTO.SpecialItemDTO item : safeList(order.getSpecialItems())) {
            lines.add(new OrderConfirmationLine(
                    "Special",
                    safeText(item.getName(), "Special"),
                    quantity(item.getQuantity()),
                    "",
                    specialDetails(item),
                    totals.specialUnitTotal(item),
                    totals.specialLineTotal(item)
            ));
        }
        return lines;
    }

    private List<String> menuDetails(OrderResponseDTO.MenuItemDTO item) {
        List<String> details = new ArrayList<>();
        for (OrderResponseDTO.MenuItemExtraDTO extra : safeList(item.getExtras())) {
            details.add(withQuantity(safeText(extra.getName(), "Extra"), extra.getQty()));
        }
        addIfPresent(details, item.getNotes());
        return details;
    }

    private List<String> pizzaDetails(OrderResponseDTO.PizzaItemDTO item) {
        List<String> details = new ArrayList<>();
        if (item.getPizzaBaseOptionName() != null && !item.getPizzaBaseOptionName().isBlank()) {
            details.add("Base: " + item.getPizzaBaseOptionName());
        }
        for (OrderResponseDTO.PizzaItemExtraDTO extra : safeList(item.getExtras())) {
            details.add(withQuantity("Extra " + safeText(extra.getIngredientName(), "topping"), extra.getQty()));
        }
        for (String removed : safeList(item.getRemovedIngredients())) {
            addIfPresent(details, "No " + removed);
        }
        addIfPresent(details, item.getNotes());
        return details;
    }

    private List<String> specialDetails(OrderResponseDTO.SpecialItemDTO item) {
        List<String> details = new ArrayList<>();
        addIfPresent(details, item.getDescription());
        for (OrderResponseDTO.SpecialSelectionDTO selection : safeList(item.getSelections())) {
            String label = safeText(selection.getLabel(), "Selection");
            String product = safeText(selection.getProductName(), "Item");
            String size = selection.getPizzaSizeCm() == null ? "" : " (" + selection.getPizzaSizeCm() + " cm)";
            details.add(label + ": " + withQuantity(product + size, selection.getQuantity()));
        }
        return details;
    }

    private String staticMapUrl(OrderResponseDTO order) {
        if (googleMapsApiKey.isBlank() || order.getLatitude() == null || order.getLongitude() == null) {
            return "";
        }
        String center = coordinate(order.getLatitude()) + "," + coordinate(order.getLongitude());
        return UriComponentsBuilder.fromUriString("https://maps.googleapis.com/maps/api/staticmap")
                .queryParam("center", center)
                .queryParam("zoom", "16")
                .queryParam("size", "960x360")
                .queryParam("scale", "2")
                .queryParam("maptype", "roadmap")
                .queryParam("markers", "color:red|" + center)
                .queryParam("key", googleMapsApiKey)
                .build()
                .encode()
                .toUriString();
    }

    private String displayAddress(OrderResponseDTO order) {
        if (order.getFormattedAddress() != null && !order.getFormattedAddress().isBlank()) {
            return order.getFormattedAddress();
        }
        return String.join(", ", List.of(
                        joinSpace(order.getHouseNumber(), order.getStreet()),
                        safeText(order.getComplexName(), ""),
                        safeText(order.getArea(), ""),
                        safeText(order.getCity(), ""),
                        safeText(order.getPostalCode(), "")))
                .replaceAll("(, )+", ", ")
                .replaceAll("^, |, $", "");
    }

    private boolean isDelivery(OrderResponseDTO order) {
        return order != null && "delivery".equalsIgnoreCase(order.getOrderType());
    }

    private String coordinate(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String joinSpace(String first, String second) {
        String left = safeText(first, "");
        String right = safeText(second, "");
        return (left + " " + right).trim();
    }

    private String withQuantity(String label, Integer quantity) {
        int amount = quantity(quantity);
        return amount == 1 ? label : label + " x" + amount;
    }

    private void addIfPresent(List<String> details, String value) {
        if (value != null && !value.isBlank()) {
            details.add(value.trim());
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int quantity(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
