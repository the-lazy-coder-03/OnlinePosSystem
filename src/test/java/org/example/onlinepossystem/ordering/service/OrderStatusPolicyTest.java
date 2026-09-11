package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.api.InvalidOrderStatusException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusPolicyTest {
    private final OrderStatusPolicy policy = new OrderStatusPolicy();

    @Test
    void acceptsPersistedPublicStatuses() {
        assertThat(policy.requireValid("Pending")).isEqualTo("Pending");
        assertThat(policy.requireValid("Preparing")).isEqualTo("Preparing");
        assertThat(policy.requireValid("Completed")).isEqualTo("Completed");
        assertThat(policy.requireValid("Rejected")).isEqualTo("Rejected");
    }

    @Test
    void rejectsUnknownAndMissingStatuses() {
        assertThatThrownBy(() -> policy.requireValid("Cancelled"))
                .isInstanceOf(InvalidOrderStatusException.class);
        assertThatThrownBy(() -> policy.requireValid(null))
                .isInstanceOf(InvalidOrderStatusException.class);
    }
}
