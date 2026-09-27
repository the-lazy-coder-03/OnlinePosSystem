package org.example.onlinepossystem.special.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;

import java.util.List;

public record SpecialQuoteRequest(@Positive @Max(50) Integer quantity,
                                  @Valid @Size(max = 100) List<Selection> selections,
                                  @Valid @Size(max = 100) List<AddonSelection> addons) {
    public record Selection(@NotNull Long componentId, @Positive @Max(100) Integer selectionIndex,
                            @NotNull @Valid OrderRequestDTO.OrderItemRequestDTO item) {}
    public record AddonSelection(@NotNull Long addonId, @Positive @Max(50) Integer quantity,
                                 @NotNull @Valid OrderRequestDTO.OrderItemRequestDTO item) {}
}
