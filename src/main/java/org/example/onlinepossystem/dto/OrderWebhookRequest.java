package org.example.onlinepossystem.dto;

import jakarta.validation.constraints.NotNull;

public record OrderWebhookRequest(@NotNull Long orderId) {
}
