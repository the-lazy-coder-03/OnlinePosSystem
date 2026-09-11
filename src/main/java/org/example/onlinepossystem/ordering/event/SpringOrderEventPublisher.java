package org.example.onlinepossystem.ordering.event;

import org.example.onlinepossystem.ordering.api.OrderEventPublisher;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringOrderEventPublisher implements OrderEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringOrderEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void orderCreated(OrderResponseDTO order, String customerUsername) {
        applicationEventPublisher.publishEvent(new OrderCreatedEvent(order, customerUsername));
    }

    @Override
    public void orderStatusChanged(OrderResponseDTO order, String customerUsername) {
        applicationEventPublisher.publishEvent(new OrderStatusChangedEvent(order, customerUsername));
    }
}
