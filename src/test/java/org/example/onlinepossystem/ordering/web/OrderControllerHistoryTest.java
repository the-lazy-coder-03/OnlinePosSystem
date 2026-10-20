package org.example.onlinepossystem.ordering.web;

import org.example.onlinepossystem.catalog.api.OrderCatalogResolver;
import org.example.onlinepossystem.ordering.api.CustomerOrderHistoryReader;
import org.example.onlinepossystem.ordering.api.CustomerOrderSummary;
import org.example.onlinepossystem.ordering.api.OrderOperations;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderControllerHistoryTest {

    @Test
    void returnsTenRecentOrdersForAuthenticatedUsername() throws Exception {
        OrderOperations operations = mock(OrderOperations.class);
        OrderCatalogResolver catalog = mock(OrderCatalogResolver.class);
        CustomerOrderHistoryReader history = mock(CustomerOrderHistoryReader.class);
        OrderController controller = new OrderController(operations, catalog, history);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("customer@example.com");
        CustomerOrderSummary order = new CustomerOrderSummary(42L, LocalDateTime.now(), "Kenridge",
                "Pending", "pickup", List.of("2 x Pizza"), new BigDecimal("189.50"));
        when(history.getRecentOrdersForCustomer("customer@example.com", 10)).thenReturn(List.of(order));

        var response = controller.getMyOrders(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(order);
        verify(history).getRecentOrdersForCustomer("customer@example.com", 10);
        assertThat(OrderController.class.getMethod("getMyOrders", Authentication.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('USER')");
    }
}
