package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.entity.PasswordResetToken;
import org.example.onlinepossystem.customer.notification.PasswordResetNotifier;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.example.onlinepossystem.customer.repository.PasswordResetTokenRepository;
import org.example.onlinepossystem.security.PasswordPolicy;
import org.example.onlinepossystem.security.api.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PasswordResetService {

    public static final String GENERIC_RESET_MESSAGE = "If that email exists, a reset link has been sent.";

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration RESET_EXPIRY = Duration.ofMinutes(30);
    private static final Duration FORGOT_PASSWORD_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_FORGOT_PASSWORD_ATTEMPTS = 5;

    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetNotifier passwordResetNotifier;
    private final RateLimiter rateLimiter;
    private final SecureRandom secureRandom;

    public PasswordResetService(
            CustomerRepository customerRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetNotifier passwordResetNotifier,
            RateLimiter rateLimiter,
            SecureRandom secureRandom
    ) {
        this.customerRepository = customerRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetNotifier = passwordResetNotifier;
        this.rateLimiter = rateLimiter;
        this.secureRandom = secureRandom;
    }

    @Transactional
    public String requestReset(String email, String clientIp) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String rateLimitKey = "forgot-password:" + clientIp + ":" + normalizedEmail;

        if (!rateLimiter.isAllowed(rateLimitKey, MAX_FORGOT_PASSWORD_ATTEMPTS, FORGOT_PASSWORD_WINDOW)) {
            logger.warn("Rate limited forgot-password request from IP {}", clientIp);
            return GENERIC_RESET_MESSAGE;
        }

        logger.info("Password reset requested from IP {}", clientIp);

        customerRepository.findByEmail(normalizedEmail).ifPresent(customer -> {
            tokenRepository.deleteByCustomer(customer);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setCustomer(customer);
            resetToken.setToken(generateToken());
            resetToken.setExpiresAt(LocalDateTime.now().plus(RESET_EXPIRY));
            tokenRepository.save(resetToken);

            passwordResetNotifier.sendResetLink(customer.getEmail(), resetToken.getToken());
        });

        return GENERIC_RESET_MESSAGE;
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (!PasswordPolicy.isValid(newPassword)) {
            throw new IllegalArgumentException(PasswordPolicy.MESSAGE);
        }

        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token)
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("Reset link is invalid or has expired."));

        Customer customer = resetToken.getCustomer();
        customer.setPassword(passwordEncoder.encode(newPassword));
        customerRepository.save(customer);

        resetToken.setUsed(true);
        tokenRepository.delete(resetToken);
        logger.info("Password reset completed for customer ID {}", customer.getId());
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
