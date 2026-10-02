package org.example.onlinepossystem.ordering.web;

import org.example.onlinepossystem.ordering.api.OrderConfirmationView;
import org.example.onlinepossystem.ordering.api.OrderPriceBreakdown;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.service.OrderConfirmationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderConfirmationControllerTest {

    private final OrderConfirmationService service = mock(OrderConfirmationService.class);
    private final OrderConfirmationController controller = new OrderConfirmationController(service);

    @Test
    void ownerGetsConfirmationView() {
        var authentication = authenticatedCustomer();
        var confirmation = confirmation();
        when(service.findForCustomer(12L, "owner@example.com")).thenReturn(Optional.of(confirmation));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.confirmation(12L, authentication, model);

        assertThat(view).isEqualTo("orderConfirmation");
        assertThat(model.get("confirmation")).isSameAs(confirmation);
    }

    @Test
    void missingOrNonOwnedOrderReturnsNotFound() {
        var authentication = authenticatedCustomer();
        when(service.findForCustomer(12L, "owner@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.confirmation(12L, authentication, new ExtendedModelMap()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode.value")
                .isEqualTo(404);
    }

    @Test
    void anonymousUserRedirectsToLogin() {
        assertThat(controller.confirmation(12L, null, new ExtendedModelMap())).isEqualTo("redirect:/login");
    }

    private OrderConfirmationView confirmation() {
        OrderResponseDTO order = new OrderResponseDTO();
        order.setId(12L);
        order.setBranchName("Kenridge");
        return new OrderConfirmationView(order, "#12", false, "Collection",
                "Your order has been received by Pete's Pizzas Kenridge.", "",
                false, "", "Delivery location map", List.of(),
                new OrderPriceBreakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false));
    }

    private UsernamePasswordAuthenticationToken authenticatedCustomer() {
        return new UsernamePasswordAuthenticationToken("owner@example.com", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
