package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.entity.PasswordResetToken;
import org.example.onlinepossystem.customer.notification.PasswordResetNotifier;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.example.onlinepossystem.customer.repository.PasswordResetTokenRepository;
import org.example.onlinepossystem.security.SimpleRateLimiter;
import org.example.onlinepossystem.security.DefaultPasswordPolicy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {
    @Test
    void resetRequestPersistsTokenAndUsesInjectedNotifier() {
        CustomerRepository customers = mock(CustomerRepository.class);
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        PasswordResetNotifier notifier = mock(PasswordResetNotifier.class);
        SimpleRateLimiter rateLimiter = new SimpleRateLimiter();
        SecureRandom secureRandom = new SecureRandom();
        PasswordResetService service = new PasswordResetService(
                customers,
                tokens,
                encoder,
                notifier,
                rateLimiter,
                secureRandom,
                new DefaultPasswordPolicy()
        );
        Customer customer = new Customer();
        customer.setEmail("customer@example.com");
        when(customers.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));

        service.requestReset(" Customer@Example.com ", "127.0.0.1");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getCustomer()).isSameAs(customer);
        assertThat(savedToken.getTokenHash()).isNotBlank();

        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(notifier).sendResetLink(org.mockito.Mockito.eq("customer@example.com"), rawTokenCaptor.capture());
        assertThat(savedToken.getTokenHash()).isEqualTo(sha256(rawTokenCaptor.getValue()));
        assertThat(savedToken.getTokenHash()).isNotEqualTo(rawTokenCaptor.getValue());
        verify(tokens).deleteByCustomerAndUsedFalse(customer);
    }

    @Test
    void resetRequestForUnknownEmailReturnsGenericMessageWithoutNotification() {
        CustomerRepository customers = mock(CustomerRepository.class);
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        PasswordResetNotifier notifier = mock(PasswordResetNotifier.class);
        PasswordResetService service = new PasswordResetService(
                customers,
                tokens,
                mock(PasswordEncoder.class),
                notifier,
                new SimpleRateLimiter(),
                new SecureRandom(),
                new DefaultPasswordPolicy()
        );
        when(customers.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        String message = service.requestReset("missing@example.com", "127.0.0.1");

        assertThat(message).isEqualTo(PasswordResetService.GENERIC_RESET_MESSAGE);
        verify(tokens, never()).save(any());
        verify(notifier, never()).sendResetLink(any(), any());
    }

    @Test
    void resetPasswordUpdatesPasswordAndInvalidatesOutstandingTokens() {
        Customer customer = new Customer();
        customer.setEmail("customer@example.com");
        customer.setPassword("{bcrypt}old");

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setCustomer(customer);
        resetToken.setTokenHash(sha256("raw-token"));
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(tokens.findByTokenHashAndUsedFalse(sha256("raw-token"))).thenReturn(Optional.of(resetToken));
        when(encoder.encode("ValidPass1!")).thenReturn("{bcrypt}new");

        PasswordResetService service = new PasswordResetService(
                mock(CustomerRepository.class),
                tokens,
                encoder,
                mock(PasswordResetNotifier.class),
                new SimpleRateLimiter(),
                new SecureRandom(),
                new DefaultPasswordPolicy()
        );

        service.resetPassword("raw-token", "ValidPass1!", "ValidPass1!");

        assertThat(customer.getPassword()).isEqualTo("{bcrypt}new");
        assertThat(resetToken.isUsed()).isTrue();
        verify(tokens).save(resetToken);
        verify(tokens).deleteByCustomerAndUsedFalse(customer);
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        Customer customer = new Customer();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setCustomer(customer);
        resetToken.setTokenHash(sha256("expired-token"));
        resetToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        when(tokens.findByTokenHashAndUsedFalse(sha256("expired-token"))).thenReturn(Optional.of(resetToken));

        PasswordResetService service = new PasswordResetService(
                mock(CustomerRepository.class),
                tokens,
                mock(PasswordEncoder.class),
                mock(PasswordResetNotifier.class),
                new SimpleRateLimiter(),
                new SecureRandom(),
                new DefaultPasswordPolicy()
        );

        assertThatThrownBy(() -> service.resetPassword("expired-token", "ValidPass1!", "ValidPass1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid or has expired");
    }

    @Test
    void resetPasswordRejectsInvalidOrUsedToken() {
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        when(tokens.findByTokenHashAndUsedFalse(sha256("used-token"))).thenReturn(Optional.empty());

        PasswordResetService service = new PasswordResetService(
                mock(CustomerRepository.class),
                tokens,
                mock(PasswordEncoder.class),
                mock(PasswordResetNotifier.class),
                new SimpleRateLimiter(),
                new SecureRandom(),
                new DefaultPasswordPolicy()
        );

        assertThatThrownBy(() -> service.resetPassword("used-token", "ValidPass1!", "ValidPass1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid or has expired");
    }

    @Test
    void resetPasswordRejectsInvalidPassword() {
        PasswordResetService service = new PasswordResetService(
                mock(CustomerRepository.class),
                mock(PasswordResetTokenRepository.class),
                mock(PasswordEncoder.class),
                mock(PasswordResetNotifier.class),
                new SimpleRateLimiter(),
                new SecureRandom(),
                new DefaultPasswordPolicy()
        );

        assertThatThrownBy(() -> service.resetPassword("token", "weak", "weak"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must be");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
