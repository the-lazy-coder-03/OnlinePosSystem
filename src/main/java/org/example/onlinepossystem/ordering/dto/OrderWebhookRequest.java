package org.example.onlinepossystem.ordering.dto;

import jakarta.validation.constraints.NotNull;

public record OrderWebhookRequest(@NotNull Long orderId) {
}
