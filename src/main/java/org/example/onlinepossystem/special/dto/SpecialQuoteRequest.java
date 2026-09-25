package org.example.onlinepossystem.special.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;

import java.util.List;

public record SpecialQuoteRequest(@Positive Integer quantity,
                                  @Valid List<Selection> selections,
                                  @Valid List<AddonSelection> addons) {
    public record Selection(@NotNull Long componentId, @Positive Integer selectionIndex,
                            @NotNull @Valid OrderRequestDTO.OrderItemRequestDTO item) {}
    public record AddonSelection(@NotNull Long addonId, @Positive Integer quantity,
                                 @NotNull @Valid OrderRequestDTO.OrderItemRequestDTO item) {}
}
