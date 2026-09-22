package org.example.onlinepossystem.customer.api;

import java.time.LocalDateTime;

public record AdminCustomerNoteView(Long id, Long customerId, String authorUsername,
                                    String body, LocalDateTime createdAt) {}
