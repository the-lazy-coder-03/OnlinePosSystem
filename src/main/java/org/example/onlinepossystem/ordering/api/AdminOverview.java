package org.example.onlinepossystem.ordering.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminOverview(
        String range,
        LocalDate startDate,
        LocalDate endDate,
        Integer branchId,
        String branchName,
        long pendingOrders,
        long preparingOrders,
        long completedOrders,
        long rejectedOrders,
        BigDecimal revenue,
        BigDecimal averageOrderValue,
        List<BranchSummary> branches
) {
    public record BranchSummary(
            Integer branchId,
            String branchName,
            long pendingOrders,
            long preparingOrders,
            long completedOrders,
            long rejectedOrders,
            BigDecimal revenue
    ) {
    }
}
