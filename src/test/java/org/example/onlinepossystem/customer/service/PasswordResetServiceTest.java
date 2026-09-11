package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.entity.PasswordResetToken;
import org.example.onlinepossystem.customer.notification.PasswordResetNotifier;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.example.onlinepossystem.customer.repository.PasswordResetTokenRepository;
import org.example.onlinepossystem.security.SimpleRateLimiter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
                secureRandom
        );
        Customer customer = new Customer();
        customer.setEmail("customer@example.com");
        when(customers.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));

        service.requestReset(" Customer@Example.com ", "127.0.0.1");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getCustomer()).isSameAs(customer);
        assertThat(savedToken.getToken()).isNotBlank();
        verify(notifier).sendResetLink("customer@example.com", savedToken.getToken());
    }
}
