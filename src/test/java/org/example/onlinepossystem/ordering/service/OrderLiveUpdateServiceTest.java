package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.dto.OrderRealtimeMessage;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OrderLiveUpdateServiceTest {

    private final SimpMessageSendingOperations messagingTemplate = mock(SimpMessageSendingOperations.class);
    private final OrderLiveUpdateService service = new OrderLiveUpdateService(messagingTemplate);

    @Test
    void createdOrderNotifiesAdminsAndCustomer() {
        OrderResponseDTO order = order("Pending");

        service.handleOrderCreated(new OrderCreatedEvent(order, "customer@example.com"));

        verify(messagingTemplate).convertAndSend("/topic/admin/orders", order);
        verify(messagingTemplate).convertAndSend("/topic/admin/branches/1/orders", order);
        verify(messagingTemplate).convertAndSendToUser(
                eq("customer@example.com"),
                eq("/queue/orders"),
                eq(new OrderRealtimeMessage(
                        "ORDER_SUBMITTED",
                        1058L,
                        1,
                        "Kenridge",
                        "Pending",
                        "Your order has been successfully submitted.",
                        order
                ))
        );
    }

    @Test
    void guestOrderDoesNotSendPrivateCustomerNotification() {
        OrderResponseDTO order = order("Pending");

        service.handleOrderCreated(new OrderCreatedEvent(order, null));

        verify(messagingTemplate).convertAndSend("/topic/admin/orders", order);
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), eq("/queue/orders"), any());
    }

    private OrderResponseDTO order(String status) {
        OrderResponseDTO order = new OrderResponseDTO();
        order.setId(1058L);
        order.setBranchId(1);
        order.setBranchName("Kenridge");
        order.setStatus(status);
        return order;
    }
}
