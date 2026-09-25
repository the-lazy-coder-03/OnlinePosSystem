package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.junit.jupiter.api.Test;

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

    private OrderRequestDTO requestWith(String gateAccessCode) {
        OrderRequestDTO request = new OrderRequestDTO();
        request.setGateAccessCode(gateAccessCode);
        OrderRequestDTO.OrderItemRequestDTO item = new OrderRequestDTO.OrderItemRequestDTO();
        item.setMenuItemId(1);
        request.setItems(List.of(item));
        return request;
    }
}
