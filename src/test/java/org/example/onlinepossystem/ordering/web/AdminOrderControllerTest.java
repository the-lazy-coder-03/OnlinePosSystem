package org.example.onlinepossystem.ordering.web;

import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.branch.api.BranchView;
import org.example.onlinepossystem.customer.api.AccountAccess;
import org.example.onlinepossystem.customer.api.AccountAccessReader;
import org.example.onlinepossystem.ordering.api.OrderOperations;
import org.example.onlinepossystem.ordering.api.OrderRealtimePublisher;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminOrderControllerTest {
    private final OrderOperations orderOperations = mock(OrderOperations.class);
    private final OrderRealtimePublisher realtimePublisher = mock(OrderRealtimePublisher.class);
    private final AccountAccessReader accessReader = mock(AccountAccessReader.class);
    private final BranchLookup branchLookup = mock(BranchLookup.class);
    private final Authentication authentication = mock(Authentication.class);
    private AdminOrderController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminOrderController(orderOperations, realtimePublisher, accessReader, branchLookup);
        when(authentication.getName()).thenReturn("branch-admin");
        when(branchLookup.requireById(1)).thenReturn(new BranchView(1, "Kenridge"));
        when(branchLookup.requireById(2)).thenReturn(new BranchView(2, "Uitzicht"));
    }

    @Test
    void branchAdminReadsOnlyTheirOwnBranch() {
        when(accessReader.findByUsername("branch-admin")).thenReturn(new AccountAccess(1));
        when(orderOperations.getOrdersByBranch("Kenridge")).thenReturn(List.of());

        assertThat(controller.getOrders(null, null, authentication).getBody()).isEmpty();

        verify(orderOperations).getOrdersByBranch("Kenridge");
    }

    @Test
    void branchAdminCannotReadOrMutateAnotherBranch() {
        when(accessReader.findByUsername("branch-admin")).thenReturn(new AccountAccess(1));

        assertThatThrownBy(() -> controller.getOrders(null, 2, authentication))
                .isInstanceOf(AccessDeniedException.class);

        OrderResponseDTO order = new OrderResponseDTO();
        order.setBranchId(2);
        when(orderOperations.getOrderById(9L)).thenReturn(order);
        assertThatThrownBy(() -> controller.updateOrderStatus(9L, java.util.Map.of("status", "Preparing"), authentication))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(realtimePublisher);
    }
}
