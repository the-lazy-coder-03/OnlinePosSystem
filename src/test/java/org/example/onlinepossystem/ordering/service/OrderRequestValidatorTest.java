package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.customer.api.AddressSelection;
import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderRequestValidatorTest {
    private final OrderRequestValidator validator = new OrderRequestValidator();

    @Test
    void acceptsSupportedGateAccessCharacters() {
        for (String code : List.of("0123#", "Gate 4*", "AB12", "")) {
            assertThatCode(() -> validator.validate(requestWith(code))).doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsUnsupportedGateAccessCharacters() {
        assertThatThrownBy(() -> validator.validate(requestWith("<script>")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("letters, numbers, spaces, # and *");
    }

    @Test
    void rejectsGateAccessCodesLongerThan64Characters() {
        assertThatThrownBy(() -> validator.validate(requestWith("A".repeat(65))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("64 characters or fewer");
    }

    @Test
    void rejectsDeliveryWithoutVerifiedGoogleAddress() {
        OrderRequestDTO request = requestWith("");
        request.setOrderType("delivery");
        request.setStreet("Typed Street");
        request.setArea("Typed Area");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verified Google address");
    }

    @Test
    void acceptsDeliveryWithVerifiedGoogleAddress() {
        OrderRequestDTO request = requestWith("");
        request.setOrderType("delivery");
        request.setDeliveryAddress(verifiedAddress());

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void acceptsDeliveryWhenOptionalGoogleComponentsAreMissing() {
        OrderRequestDTO request = requestWith("");
        request.setOrderType("delivery");
        request.setDeliveryAddress(new AddressSelection(
                "places/test",
                "Main Street, Kenridge, Cape Town, South Africa",
                new BigDecimal("-33.861000"),
                new BigDecimal("18.650000"),
                null,
                "Main Street",
                "Kenridge",
                "Cape Town",
                null,
                null,
                null,
                "South Africa"
        ));

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    private OrderRequestDTO requestWith(String gateAccessCode) {
        OrderRequestDTO request = new OrderRequestDTO();
        request.setGateAccessCode(gateAccessCode);
        OrderRequestDTO.OrderItemRequestDTO item = new OrderRequestDTO.OrderItemRequestDTO();
        item.setMenuItemId(1);
        request.setItems(List.of(item));
        return request;
    }

    private AddressSelection verifiedAddress() {
        return new AddressSelection(
                "places/test",
                "12 Main Street, Kenridge, Cape Town, 7550, South Africa",
                new BigDecimal("-33.861000"),
                new BigDecimal("18.650000"),
                "12",
                "Main Street",
                "Kenridge",
                "Cape Town",
                "7550",
                null,
                "Western Cape",
                "South Africa"
        );
    }
}
