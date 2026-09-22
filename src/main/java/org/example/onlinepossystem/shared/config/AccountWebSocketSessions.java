package org.example.onlinepossystem.shared.config;

import org.example.onlinepossystem.customer.api.AccountAccessChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AccountWebSocketSessions {
    private static final Logger logger = LoggerFactory.getLogger(AccountWebSocketSessions.class);
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessions.put(session.getId(), session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                sessions.remove(session.getId());
                super.afterConnectionClosed(session, status);
            }
        };
    }

    @TransactionalEventListener
    public void accessChanged(AccountAccessChanged event) {
        sessions.values().stream().filter(s -> s.getPrincipal() != null
                && Objects.equals(s.getPrincipal().getName(), event.username())).forEach(session -> {
            try { session.close(CloseStatus.POLICY_VIOLATION); }
            catch (IOException exception) { logger.warn("Could not close an expired account WebSocket session"); }
        });
    }
}
