package org.example.onlinepossystem.ordering.api;

import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;

import java.math.BigDecimal;
import java.util.Optional;

public interface PersistedOrderSnapshotReader {
    Optional<Snapshot> findById(long orderId);

    record Snapshot(OrderResponseDTO order, BigDecimal total) {}
}
