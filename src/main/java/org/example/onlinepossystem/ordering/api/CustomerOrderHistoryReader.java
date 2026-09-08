package org.example.onlinepossystem.ordering.api;

import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;

import java.util.List;

public interface CustomerOrderHistoryReader {
    List<OrderResponseDTO> getRecentOrdersForCustomer(String customerEmail, int limit);
}
