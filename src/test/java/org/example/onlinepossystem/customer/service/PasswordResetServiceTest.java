package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.notification.PasswordResetNotifier;
import org.example.onlinepossystem.customer.persistence.AccountBootstrapStore;
import org.example.onlinepossystem.notification.email.NotificationDeliveryException;
import org.example.onlinepossystem.security.DefaultPasswordPolicy;
import org.example.onlinepossystem.security.SimpleRateLimiter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {
    private final AccountBootstrapStore accounts = mock(AccountBootstrapStore.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final PasswordResetNotifier notifier = mock(PasswordResetNotifier.class);
    private final PasswordResetService service = new PasswordResetService(accounts, encoder, notifier,
            new SimpleRateLimiter(), new SecureRandom(), new DefaultPasswordPolicy());

    @Test
    void storesOnlyTokenHashAndSendsRawTokenThroughNotifier() throws Exception {
        when(accounts.createReset(eq("customer@example.com"), anyString(), any()))
                .thenReturn(Optional.of("customer@example.com"));
        service.requestReset(" Customer@Example.com ", "127.0.0.1");
        var hash = ArgumentCaptor.forClass(String.class);
        var raw = ArgumentCaptor.forClass(String.class);
        var expiry = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(accounts).createReset(eq("customer@example.com"), hash.capture(), expiry.capture());
        verify(notifier).sendResetLink(eq("customer@example.com"), raw.capture());
        assertThat(hash.getValue()).isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getValue().getBytes(StandardCharsets.UTF_8)))).isNotEqualTo(raw.getValue());
        assertThat(expiry.getValue()).isBetween(LocalDateTime.now().plusMinutes(29), LocalDateTime.now().plusMinutes(31));
    }

    @Test
    void unknownEmailUsesGenericResponseAndSendsNothing() {
        when(accounts.createReset(anyString(), anyString(), any())).thenReturn(Optional.empty());
        assertThat(service.requestReset("missing@example.com", "127.0.0.1"))
                .isEqualTo(PasswordResetService.GENERIC_RESET_MESSAGE);
        verifyNoInteractions(notifier);
    }

    @Test
    void deliveryFailureCancelsOnlyTheNewToken() {
        when(accounts.createReset(anyString(), anyString(), any())).thenReturn(Optional.of("customer@example.com"));
        doThrow(new NotificationDeliveryException("Unavailable")).when(notifier).sendResetLink(anyString(), anyString());
        assertThat(service.requestReset("customer@example.com", "127.0.0.1"))
                .isEqualTo(PasswordResetService.GENERIC_RESET_MESSAGE);
        var hash = ArgumentCaptor.forClass(String.class);
        verify(accounts).createReset(anyString(), hash.capture(), any());
        verify(accounts).cancelReset(hash.getValue());
    }

    @Test
    void consumesTokenWithEncodedPassword() {
        when(encoder.encode("ValidPass1!")).thenReturn("{bcrypt}new");
        when(accounts.consumeReset(anyString(), eq("{bcrypt}new"))).thenReturn(true);
        service.resetPassword("raw-token", "ValidPass1!", "ValidPass1!");
        verify(accounts).consumeReset(matches("[a-f0-9]{64}"), eq("{bcrypt}new"));
    }

    @Test
    void invalidExpiredOrUsedTokenFails() {
        when(accounts.consumeReset(anyString(), any())).thenReturn(false);
        assertThatThrownBy(() -> service.resetPassword("expired-token", "ValidPass1!"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("invalid or has expired");
    }

    @Test
    void invalidPasswordFailsBeforePersistence() {
        assertThatThrownBy(() -> service.resetPassword("token", "weak", "weak"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Password must be");
        verifyNoInteractions(accounts);
    }

    @Test
    void mismatchedConfirmationFailsBeforePersistence() {
        assertThatThrownBy(() -> service.resetPassword("token", "ValidPass1!", "different"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("do not match");
        verifyNoInteractions(accounts);
    }
}
