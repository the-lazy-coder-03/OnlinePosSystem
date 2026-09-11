package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.api.InvalidOrderStatusException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OrderStatusPolicy {
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "Pending",
            "Preparing",
            "Completed",
            "Rejected"
    );

    public String requireValid(String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new InvalidOrderStatusException();
        }
        return status;
    }
}
