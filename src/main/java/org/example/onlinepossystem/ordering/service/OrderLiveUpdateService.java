package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.dto.OrderRealtimeMessage;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.api.OrderRealtimePublisher;
import org.example.onlinepossystem.ordering.event.OrderCreatedEvent;
import org.example.onlinepossystem.ordering.event.OrderStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class OrderLiveUpdateService implements OrderRealtimePublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderLiveUpdateService.class);
    private static final String ADMIN_ORDERS_TOPIC = "/topic/admin/orders";
    private static final String ADMIN_BRANCH_TOPIC_PREFIX = "/topic/admin/branches/";
    private static final String CUSTOMER_ORDERS_QUEUE = "/queue/orders";

    private final SimpMessageSendingOperations messagingTemplate;

    public OrderLiveUpdateService(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async("realtimeTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        publishToAdmins(event.order());
        sendOrderSubmittedToCustomer(event.order(), event.customerUsername());
    }

    @Async("realtimeTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        publishToAdmins(event.order());
        sendStatusToCustomer(event.order(), event.customerUsername());
    }

    @Override
    public void publishToAdmins(OrderResponseDTO order) {
        if (order == null || order.getId() == null) {
            return;
        }

        try {
            messagingTemplate.convertAndSend(ADMIN_ORDERS_TOPIC, order);
            if (order.getBranchId() != null) {
                messagingTemplate.convertAndSend(ADMIN_BRANCH_TOPIC_PREFIX + order.getBranchId() + "/orders", order);
            }
            logger.info("Sent realtime admin order update. orderId={} status={}", order.getId(), order.getStatus());
        } catch (RuntimeException ex) {
            logger.warn("Could not send realtime admin order update. orderId={}", order.getId(), ex);
        }
    }

    private void sendOrderSubmittedToCustomer(OrderResponseDTO order, String customerUsername) {
        sendCustomerMessage(order, customerUsername, "ORDER_SUBMITTED", "Your order has been successfully submitted.");
    }

    private void sendStatusToCustomer(OrderResponseDTO order, String customerUsername) {
        sendCustomerMessage(order, customerUsername, eventType(order), customerMessage(order));
    }

    private void sendCustomerMessage(OrderResponseDTO order, String customerUsername, String eventType, String message) {
        if (order == null || order.getId() == null || customerUsername == null || customerUsername.isBlank()) {
            return;
        }

        OrderRealtimeMessage payload = new OrderRealtimeMessage(
                eventType,
                order.getId(),
                order.getBranchId(),
                order.getBranchName(),
                order.getStatus(),
                message,
                order
        );
        try {
            messagingTemplate.convertAndSendToUser(customerUsername, CUSTOMER_ORDERS_QUEUE, payload);
            logger.info("Sent realtime customer order update. orderId={} user={} eventType={}",
                    order.getId(), customerUsername, eventType);
        } catch (RuntimeException ex) {
            logger.warn("Could not send realtime customer order update. orderId={} user={}",
                    order.getId(), customerUsername, ex);
        }
    }

    private String eventType(OrderResponseDTO order) {
        return switch (safeStatus(order)) {
            case "Preparing" -> "ORDER_PREPARING";
            case "Completed" -> "ORDER_COMPLETED";
            case "Rejected" -> "ORDER_REJECTED";
            default -> "ORDER_SUBMITTED";
        };
    }

    private String customerMessage(OrderResponseDTO order) {
        return switch (safeStatus(order)) {
            case "Preparing" -> "Your order is being prepared.";
            case "Completed" -> "Your order has been completed.";
            case "Rejected" -> "Your order has been rejected.";
            default -> "Your order has been successfully submitted.";
        };
    }

    private String safeStatus(OrderResponseDTO order) {
        return order == null || order.getStatus() == null ? "Pending" : order.getStatus();
    }
}
