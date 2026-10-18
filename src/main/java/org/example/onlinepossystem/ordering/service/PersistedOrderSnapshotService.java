package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.api.PersistedOrderSnapshotReader;
import org.example.onlinepossystem.ordering.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PersistedOrderSnapshotService implements PersistedOrderSnapshotReader {
    private final OrderRepository orders;
    private final OrderResponseMapper mapper;
    private final OrderTotalCalculator totals;

    public PersistedOrderSnapshotService(OrderRepository orders, OrderResponseMapper mapper,
                                         OrderTotalCalculator totals) {
        this.orders = orders;
        this.mapper = mapper;
        this.totals = totals;
    }

    @Override
    public Optional<Snapshot> findById(long orderId) {
        return orders.findById(orderId).map(mapper::toDto).map(order -> new Snapshot(order, totals.total(order)));
    }
}
