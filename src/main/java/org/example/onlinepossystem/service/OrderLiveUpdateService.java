package org.example.onlinepossystem.service;

import org.example.onlinepossystem.dto.OrderResponseDTO;
import org.example.onlinepossystem.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class OrderLiveUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(OrderLiveUpdateService.class);
    private static final long STREAM_TIMEOUT_MS = Duration.ofHours(6).toMillis();

    private final List<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();

    public SseEmitter connectAdminStream() {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        adminEmitters.add(emitter);

        emitter.onCompletion(() -> adminEmitters.remove(emitter));
        emitter.onTimeout(() -> adminEmitters.remove(emitter));
        emitter.onError(error -> adminEmitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            adminEmitters.remove(emitter);
        }

        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        sendOrderToAdmins(event.order());
    }

    public void sendOrderToAdmins(OrderResponseDTO order) {
        int delivered = 0;
        for (SseEmitter emitter : adminEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("order-created")
                        .id(String.valueOf(order.getId()))
                        .data(order));
                delivered++;
            } catch (IOException | IllegalStateException ex) {
                adminEmitters.remove(emitter);
            }
        }

        logger.info("Sent new order event to admin stream. orderId={} delivered={}", order.getId(), delivered);
    }
}
