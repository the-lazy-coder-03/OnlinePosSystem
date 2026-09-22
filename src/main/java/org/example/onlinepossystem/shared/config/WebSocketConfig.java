package org.example.onlinepossystem.shared.config;

import org.example.onlinepossystem.customer.api.AccountAccess;
import org.example.onlinepossystem.customer.api.AccountAccessReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final String ALL_ADMIN_ORDERS = "/topic/admin/orders";
    private static final String BRANCH_ADMIN_PREFIX = "/topic/admin/branches/";

    private final AccountAccessReader accountAccessReader;
    private final AccountWebSocketSessions sessions;

    public WebSocketConfig(AccountAccessReader accountAccessReader, AccountWebSocketSessions sessions) {
        this.accountAccessReader = accountAccessReader;
        this.sessions = sessions;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new org.springframework.security.messaging.context.SecurityContextChannelInterceptor());
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
                    return message;
                }
                String destination = accessor.getDestination();
                if (destination == null || !destination.startsWith("/topic/admin/")) {
                    return message;
                }
                if (accessor.getUser() == null) {
                    throw new AccessDeniedException("Admin authentication is required for this subscription.");
                }
                AccountAccess access = accountAccessReader.findByUsername(accessor.getUser().getName());
                if (ALL_ADMIN_ORDERS.equals(destination)) {
                    if (!access.isSuperAdmin()) {
                        throw new AccessDeniedException("Only super admins can subscribe to all branch orders.");
                    }
                    return message;
                }
                Integer branchId = branchId(destination);
                if (branchId == null || !access.canAccessBranch(branchId)) {
                    throw new AccessDeniedException("This admin account cannot subscribe to the requested branch.");
                }
                return message;
            }
        });
    }

    @Override
    public void configureWebSocketTransport(org.springframework.web.socket.config.annotation.WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(sessions::decorate);
    }

    private Integer branchId(String destination) {
        if (!destination.startsWith(BRANCH_ADMIN_PREFIX) || !destination.endsWith("/orders")) {
            return null;
        }
        String value = destination.substring(
                BRANCH_ADMIN_PREFIX.length(),
                destination.length() - "/orders".length()
        );
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
