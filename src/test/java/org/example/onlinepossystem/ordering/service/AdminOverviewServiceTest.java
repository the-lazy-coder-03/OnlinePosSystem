package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.branch.api.BranchView;
import org.example.onlinepossystem.customer.api.AccountAccess;
import org.example.onlinepossystem.ordering.api.AdminOverview;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.entity.Order;
import org.example.onlinepossystem.ordering.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminOverviewServiceTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final OrderResponseMapper mapper = mock(OrderResponseMapper.class);
    private final BranchLookup branches = mock(BranchLookup.class);
    private final AdminOverviewService service = new AdminOverviewService(
            repository, mapper, new OrderTotalCalculator(), branches
    );

    @BeforeEach
    void setUpBranches() {
        when(branches.findAll()).thenReturn(List.of(
                new BranchView(1, "Kenridge"),
                new BranchView(2, "Uitzicht")
        ));
        when(branches.requireById(1)).thenReturn(new BranchView(1, "Kenridge"));
        when(branches.requireById(2)).thenReturn(new BranchView(2, "Uitzicht"));
    }

    @Test
    void recognizesOnlyCompletedRevenueAndKeepsOpenCountsLive() {
        Order completed = order(1, "Completed");
        Order rejected = order(2, "Rejected");
        Order pending = order(1, "Created");
        Order preparing = order(2, "Preparing");
        when(repository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(completed, rejected));
        when(repository.findByStatusInOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(pending, preparing));

        OrderResponseDTO completedDto = new OrderResponseDTO();
        OrderResponseDTO.MenuItemDTO line = new OrderResponseDTO.MenuItemDTO();
        line.setQty(2);
        line.setUnitPriceAtTime(25.0);
        completedDto.setMenuItems(List.of(line));
        when(mapper.toDto(completed)).thenReturn(completedDto);

        AdminOverview overview = service.getOverview(new AccountAccess(3), "today", null, null, null);

        assertThat(overview.pendingOrders()).isEqualTo(1);
        assertThat(overview.preparingOrders()).isEqualTo(1);
        assertThat(overview.completedOrders()).isEqualTo(1);
        assertThat(overview.rejectedOrders()).isEqualTo(1);
        assertThat(overview.revenue()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(overview.averageOrderValue()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(overview.branches()).hasSize(2);
    }

    @Test
    void branchAdminsAreLockedToTheirBranchAndCustomRangesAreInclusive() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 3);
        when(repository.findByBranchIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(1), any(), any())).thenReturn(List.of());
        when(repository.findByBranchIdAndStatusInOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(1), any())).thenReturn(List.of());

        AdminOverview overview = service.getOverview(new AccountAccess(1), "custom", start, end, null);

        assertThat(overview.branchId()).isEqualTo(1);
        assertThat(overview.startDate()).isEqualTo(start);
        assertThat(overview.endDate()).isEqualTo(end);
        assertThatThrownBy(() -> service.getOverview(new AccountAccess(1), "today", null, null, 2))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Order order(int branchId, String status) {
        Order order = new Order();
        order.setBranchId(branchId);
        order.setStatus(status);
        return order;
    }
}
