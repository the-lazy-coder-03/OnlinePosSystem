package org.example.onlinepossystem.ordering.api;

import java.util.List;

public interface CustomerOrderHistoryReader {
    List<CustomerOrderSummary> getRecentOrdersForCustomer(String customerEmail, int limit);
}
