package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.event.OrderCreatedEvent;
import org.example.onlinepossystem.shared.config.AsyncConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(classes = {OrderRealtimeTransactionTest.Config.class, AsyncConfig.class})
class OrderRealtimeTransactionTest {

    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final TestMessagingOperations messagingOperations;

    @Autowired
    OrderRealtimeTransactionTest(PlatformTransactionManager transactionManager,
                                 ApplicationEventPublisher eventPublisher,
                                 TestMessagingOperations messagingOperations) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.eventPublisher = eventPublisher;
        this.messagingOperations = messagingOperations;
    }

    @BeforeEach
    void resetMessages() {
        messagingOperations.clear();
    }

    @Test
    void orderCreatedEventDoesNotNotifyBeforeTransactionCommit() throws InterruptedException {
        OrderResponseDTO order = new OrderResponseDTO();
        order.setId(2091L);
        order.setBranchId(1);
        order.setBranchName("Kenridge");
        order.setStatus("Pending");

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new OrderCreatedEvent(order, "customer@example.com"));

            try {
                org.junit.jupiter.api.Assertions.assertFalse(messagingOperations.awaitAtLeast(1, Duration.ofMillis(100)));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
        });

        assertTrue(messagingOperations.awaitAtLeast(1, Duration.ofSeconds(2)));
        assertTrue(messagingOperations.destinations().contains("/topic/admin/orders"));
    }

    @Test
    void rolledBackOrderNeverPublishesAnEvent() throws InterruptedException {
        var order = new OrderResponseDTO();
        order.setId(2092L);
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new OrderCreatedEvent(order, "customer@example.com"));
            status.setRollbackOnly();
        });
        org.junit.jupiter.api.Assertions.assertFalse(messagingOperations.awaitAtLeast(1, Duration.ofMillis(100)));
    }

    @Configuration
    @org.springframework.transaction.annotation.EnableTransactionManagement
    static class Config {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new TestTransactionManager();
        }

        @Bean
        TestMessagingOperations messagingOperations() {
            return new TestMessagingOperations();
        }

        @Bean
        OrderLiveUpdateService orderLiveUpdateService(SimpMessageSendingOperations messagingOperations) {
            return new OrderLiveUpdateService(messagingOperations);
        }
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    static class TestMessagingOperations implements SimpMessageSendingOperations {
        private final List<String> destinations = new CopyOnWriteArrayList<>();
        private volatile CountDownLatch latch = new CountDownLatch(1);

        void clear() {
            destinations.clear();
            latch = new CountDownLatch(1);
        }

        int sentCount() {
            return destinations.size();
        }

        List<String> destinations() {
            return destinations;
        }

        boolean awaitAtLeast(int messageCount, Duration timeout) throws InterruptedException {
            if (destinations.size() >= messageCount) {
                return true;
            }
            return latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS) && destinations.size() >= messageCount;
        }

        @Override
        public void send(Message<?> message) {
        }

        @Override
        public void send(String destination, Message<?> message) {
            record(destination);
        }

        @Override
        public void convertAndSend(Object payload) {
        }

        @Override
        public void convertAndSend(String destination, Object payload) {
            record(destination);
        }

        @Override
        public void convertAndSend(String destination, Object payload, Map<String, Object> headers) {
            record(destination);
        }

        @Override
        public void convertAndSend(Object payload, MessagePostProcessor postProcessor) {
        }

        @Override
        public void convertAndSend(String destination, Object payload, MessagePostProcessor postProcessor) {
            record(destination);
        }

        @Override
        public void convertAndSend(String destination, Object payload, Map<String, Object> headers,
                                   MessagePostProcessor postProcessor) {
            record(destination);
        }

        @Override
        public void convertAndSendToUser(String user, String destination, Object payload) {
            record(destination);
        }

        @Override
        public void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers) {
            record(destination);
        }

        @Override
        public void convertAndSendToUser(String user, String destination, Object payload,
                                         MessagePostProcessor postProcessor) {
            record(destination);
        }

        @Override
        public void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers,
                                         MessagePostProcessor postProcessor) {
            record(destination);
        }

        private void record(String destination) {
            destinations.add(destination);
            latch.countDown();
        }
    }
}
