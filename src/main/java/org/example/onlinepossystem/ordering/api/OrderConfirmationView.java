package org.example.onlinepossystem.ordering.api;

import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;

import java.util.List;

public record OrderConfirmationView(
        OrderResponseDTO order,
        String orderNumber,
        boolean delivery,
        String orderTypeLabel,
        String receivedMessage,
        String displayAddress,
        boolean showMap,
        String staticMapUrl,
        String mapAltText,
        List<OrderConfirmationLine> lines,
        OrderPriceBreakdown totals
) {
}
