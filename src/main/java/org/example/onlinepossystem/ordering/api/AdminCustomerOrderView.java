package org.example.onlinepossystem.ordering.api;

import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;

import java.math.BigDecimal;

public record AdminCustomerOrderView(OrderResponseDTO order, BigDecimal total) {}
