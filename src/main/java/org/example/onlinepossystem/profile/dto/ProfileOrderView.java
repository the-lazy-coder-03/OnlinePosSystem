package org.example.onlinepossystem.profile.dto;

import java.util.List;

public record ProfileOrderView(
        Long id,
        String date,
        String branchName,
        String status,
        String orderType,
        List<String> itemLines,
        String formattedTotal
) {
}
