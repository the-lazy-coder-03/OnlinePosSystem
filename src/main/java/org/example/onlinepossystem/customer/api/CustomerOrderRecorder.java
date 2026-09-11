package org.example.onlinepossystem.customer.api;

import java.time.LocalDateTime;

public interface CustomerOrderRecorder {
    void recordOrderPlaced(Long customerId, LocalDateTime orderedAt);
}
