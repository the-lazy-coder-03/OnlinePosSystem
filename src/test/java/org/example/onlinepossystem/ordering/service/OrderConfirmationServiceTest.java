package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.customer.api.CustomerAccount;
import org.example.onlinepossystem.customer.api.CustomerAccountReader;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.entity.Order;
import org.example.onlinepossystem.ordering.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderConfirmationServiceTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final CustomerAccountReader customers = mock(CustomerAccountReader.class);
    private final OrderResponseMapper mapper = mock(OrderResponseMapper.class);
    private final OrderTotalCalculator totals = new OrderTotalCalculator();
    private final OrderConfirmationService service = new OrderConfirmationService(
            orders, customers, mapper, totals, "maps-key");

    @Test
    void buildsDeliveryConfirmationWithStaticMapFromPersistedCoordinates() {
        CustomerAccount customer = customer(42L, "owner@example.com");
        Order entity = new Order();
        OrderResponseDTO dto = deliveryOrder();
        OrderResponseDTO.MenuItemDTO item = new OrderResponseDTO.MenuItemDTO();
        item.setMenuItemName("Cheeseburger");
        item.setQty(2);
        item.setUnitPriceAtTime(80.0);
        OrderResponseDTO.MenuItemExtraDTO extra = new OrderResponseDTO.MenuItemExtraDTO();
        extra.setName("Extra sauce");
        extra.setQty(1);
        extra.setUnitPriceAtTime(5.0);
        item.setExtras(List.of(extra));
        dto.setMenuItems(List.of(item));

        when(customers.findByEmail("owner@example.com")).thenReturn(Optional.of(customer));
        when(orders.findByIdAndCustomerId(99L, 42L)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        var view = service.findForCustomer(99L, "owner@example.com").orElseThrow();

        assertThat(view.delivery()).isTrue();
        assertThat(view.orderNumber()).isEqualTo("#99");
        assertThat(view.receivedMessage()).isEqualTo("Your order has been received by Pete's Pizzas Kenridge.");
        assertThat(view.displayAddress()).isEqualTo("12 Main Street, Kenridge, Cape Town, 7550, South Africa");
        assertThat(view.showMap()).isTrue();
        assertThat(view.staticMapUrl()).contains("staticmap", "center=-33.861,18.65", "markers=color:red%7C-33.861,18.65");
        assertThat(view.lines()).hasSize(1);
        assertThat(view.lines().get(0).lineTotal()).isEqualByComparingTo("170.00");
        assertThat(view.totals().total()).isEqualByComparingTo("170.00");
        verify(orders).findByIdAndCustomerId(99L, 42L);
    }

    @Test
    void returnsEmptyWhenOrderDoesNotBelongToCustomer() {
        when(customers.findByEmail("owner@example.com")).thenReturn(Optional.of(customer(42L, "owner@example.com")));
        when(orders.findByIdAndCustomerId(99L, 42L)).thenReturn(Optional.empty());

        assertThat(service.findForCustomer(99L, "owner@example.com")).isEmpty();
    }

    @Test
    void collectionConfirmationHidesDeliveryMap() {
        CustomerAccount customer = customer(42L, "owner@example.com");
        Order entity = new Order();
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(100L);
        dto.setBranchName("Uitzicht");
        dto.setOrderType("pickup");
        dto.setCustomerName("Owner");
        dto.setPhone("0712345678");

        when(customers.findByEmail("owner@example.com")).thenReturn(Optional.of(customer));
        when(orders.findByIdAndCustomerId(100L, 42L)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        var view = service.findForCustomer(100L, "owner@example.com").orElseThrow();

        assertThat(view.delivery()).isFalse();
        assertThat(view.orderTypeLabel()).isEqualTo("Collection");
        assertThat(view.showMap()).isFalse();
        assertThat(view.staticMapUrl()).isBlank();
    }

    @Test
    void deliveryWithoutCoordinatesFallsBackToAddressOnly() {
        OrderConfirmationService noKeyService = new OrderConfirmationService(
                orders, customers, mapper, totals, "");
        CustomerAccount customer = customer(42L, "owner@example.com");
        Order entity = new Order();
        OrderResponseDTO dto = deliveryOrder();
        dto.setLatitude(null);
        dto.setLongitude(null);

        when(customers.findByEmail("owner@example.com")).thenReturn(Optional.of(customer));
        when(orders.findByIdAndCustomerId(99L, 42L)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        var view = noKeyService.findForCustomer(99L, "owner@example.com").orElseThrow();

        assertThat(view.delivery()).isTrue();
        assertThat(view.showMap()).isFalse();
        assertThat(view.displayAddress()).contains("Main Street");
    }

    private OrderResponseDTO deliveryOrder() {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(99L);
        dto.setBranchName("Kenridge");
        dto.setOrderType("delivery");
        dto.setCustomerName("Owner");
        dto.setPhone("0712345678");
        dto.setFormattedAddress("12 Main Street, Kenridge, Cape Town, 7550, South Africa");
        dto.setLatitude(new BigDecimal("-33.861000"));
        dto.setLongitude(new BigDecimal("18.650000"));
        return dto;
    }

    private CustomerAccount customer(Long id, String email) {
        return new CustomerAccount(id, "0712345678", null, email, "12", "Main Street", "Kenridge",
                null, null, "Kenridge", "Cape Town", "7550", "places/customer",
                "12 Main Street, Kenridge, Cape Town, 7550, South Africa",
                new BigDecimal("-33.861000"), new BigDecimal("18.650000"),
                "Western Cape", "South Africa", "Owner", "Customer", "USER", 0);
    }
}
