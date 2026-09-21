package org.example.onlinepossystem.shared.config;

import org.example.onlinepossystem.customer.api.AccountAccess;
import org.example.onlinepossystem.customer.api.AccountAccessReader;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    @Test
    void branchAdminCanSubscribeOnlyToTheirBranchTopic() {
        AccountAccessReader accessReader = mock(AccountAccessReader.class);
        when(accessReader.findByUsername("kenridge-admin")).thenReturn(new AccountAccess(1));
        ChannelInterceptor interceptor = interceptor(new WebSocketConfig(accessReader));

        Message<?> ownBranch = subscription("/topic/admin/branches/1/orders", "kenridge-admin");
        assertThat(interceptor.preSend(ownBranch, mock(MessageChannel.class))).isSameAs(ownBranch);
        assertThatThrownBy(() -> interceptor.preSend(
                subscription("/topic/admin/branches/2/orders", "kenridge-admin"), mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(
                subscription("/topic/admin/orders", "kenridge-admin"), mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void superAdminCanSubscribeToCombinedTopic() {
        AccountAccessReader accessReader = mock(AccountAccessReader.class);
        when(accessReader.findByUsername("super-admin")).thenReturn(new AccountAccess(3));
        ChannelInterceptor interceptor = interceptor(new WebSocketConfig(accessReader));
        Message<?> combined = subscription("/topic/admin/orders", "super-admin");

        assertThat(interceptor.preSend(combined, mock(MessageChannel.class))).isSameAs(combined);
    }

    private ChannelInterceptor interceptor(WebSocketConfig config) {
        ChannelRegistration registration = mock(ChannelRegistration.class);
        AtomicReference<ChannelInterceptor> captured = new AtomicReference<>();
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return registration;
        }).when(registration).interceptors(any(ChannelInterceptor[].class));
        config.configureClientInboundChannel(registration);
        return captured.get();
    }

    private Message<?> subscription(String destination, String username) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(new TestingAuthenticationToken(username, "n/a"));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
