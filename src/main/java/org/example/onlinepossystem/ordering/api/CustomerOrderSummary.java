package org.example.onlinepossystem.ordering.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerOrderSummary(
        Long id,
        LocalDateTime createdAt,
        String branchName,
        String status,
        String orderType,
        List<String> itemLines,
        BigDecimal total
) {
}
