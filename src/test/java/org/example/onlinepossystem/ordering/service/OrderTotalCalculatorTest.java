package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTotalCalculatorTest {

    @Test
    void totalsStoredPricesAndQuantitiesForAllSupportedLineTypes() {
        OrderResponseDTO order = new OrderResponseDTO();
        OrderResponseDTO.MenuItemDTO burger = new OrderResponseDTO.MenuItemDTO();
        burger.setQty(2);
        burger.setUnitPriceAtTime(80.0);
        OrderResponseDTO.MenuItemExtraDTO burgerExtra = new OrderResponseDTO.MenuItemExtraDTO();
        burgerExtra.setQty(2);
        burgerExtra.setUnitPriceAtTime(7.5);
        burger.setExtras(List.of(burgerExtra));

        OrderResponseDTO.PizzaItemDTO pizza = new OrderResponseDTO.PizzaItemDTO();
        pizza.setQty(2);
        pizza.setBasePriceAtTime(100.0);
        pizza.setPizzaBaseOptionPriceAtTime(20.0);
        OrderResponseDTO.PizzaItemExtraDTO topping = new OrderResponseDTO.PizzaItemExtraDTO();
        topping.setQty(3);
        topping.setUnitPriceAtTime(5.0);
        pizza.setExtras(List.of(topping));
        order.setMenuItems(List.of(burger));
        order.setPizzaItems(List.of(pizza));

        OrderTotalCalculator calculator = new OrderTotalCalculator();
        assertThat(calculator.menuItemUnitTotal(burger)).isEqualByComparingTo(new BigDecimal("95.00"));
        assertThat(calculator.menuItemLineTotal(burger)).isEqualByComparingTo(new BigDecimal("190.00"));
        assertThat(calculator.pizzaItemUnitTotal(pizza)).isEqualByComparingTo(new BigDecimal("135.00"));
        assertThat(calculator.pizzaItemLineTotal(pizza)).isEqualByComparingTo(new BigDecimal("270.00"));
        assertThat(calculator.total(order)).isEqualByComparingTo(new BigDecimal("460.00"));
        assertThat(calculator.breakdown(order).subtotal()).isEqualByComparingTo(new BigDecimal("460.00"));
        assertThat(calculator.breakdown(order).deliveryFee()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(calculator.breakdown(order).total()).isEqualByComparingTo(new BigDecimal("460.00"));
    }
}
