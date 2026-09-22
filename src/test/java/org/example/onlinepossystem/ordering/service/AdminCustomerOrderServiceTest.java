package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.customer.api.AccountAccess;
import org.example.onlinepossystem.customer.api.CustomerRecordLookup;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.entity.Order;
import org.example.onlinepossystem.ordering.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminCustomerOrderServiceTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final CustomerRecordLookup customers = mock(CustomerRecordLookup.class);
    private final OrderResponseMapper mapper = mock(OrderResponseMapper.class);
    private final AdminCustomerOrderService service = new AdminCustomerOrderService(
            orders, customers, mapper, new OrderTotalCalculator());

    @Test
    void branchAdminQueriesOnlyTheirBranchAndUsesStoredPrice() {
        Order order = new Order();
        order.setId(10L);
        order.setCustomerId(7L);
        order.setBranchId(1);
        OrderResponseDTO dto = new OrderResponseDTO();
        OrderResponseDTO.MenuItemDTO item = new OrderResponseDTO.MenuItemDTO();
        item.setQty(2);
        item.setUnitPriceAtTime(45.0);
        dto.setMenuItems(List.of(item));
        when(mapper.toDto(order)).thenReturn(dto);
        when(orders.findAllByCustomerIdAndBranchIdOrderByCreatedAtDescIdDesc(eq(7L), eq(1), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        var result = service.history(7L, 0, new AccountAccess(1));
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).total()).isEqualByComparingTo("90.00");
        verify(orders).findAllByCustomerIdAndBranchIdOrderByCreatedAtDescIdDesc(eq(7L), eq(1), any(Pageable.class));
    }

    @Test
    void superAdminCanSeeBothBranchesAndEmptyHistory() {
        when(orders.findAllByCustomerIdOrderByCreatedAtDescIdDesc(eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        assertThat(service.history(7L, 0, new AccountAccess(3)).items()).isEmpty();
        verify(orders).findAllByCustomerIdOrderByCreatedAtDescIdDesc(eq(7L), any(Pageable.class));
    }

    @Test
    void usersAndDriversCannotReadHistory() {
        assertThatThrownBy(() -> service.history(7L, 0, new AccountAccess(0)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> service.history(7L, 0, new AccountAccess(4)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(orders, customers);
    }
}
