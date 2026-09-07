package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.entity.PasswordResetToken;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.example.onlinepossystem.customer.repository.PasswordResetTokenRepository;
import org.example.onlinepossystem.security.PasswordPolicy;
import org.example.onlinepossystem.security.SimpleRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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
    private final JavaMailSender mailSender;
    private final SimpleRateLimiter rateLimiter;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String appBaseUrl;
    private final String mailFrom;

    public PasswordResetService(
            CustomerRepository customerRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            SimpleRateLimiter rateLimiter,
            @Value("${app.base-url:http://localhost:8080}") String appBaseUrl,
            @Value("${spring.mail.username:no-reply@localhost}") String mailFrom
    ) {
        this.customerRepository = customerRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.rateLimiter = rateLimiter;
        this.appBaseUrl = appBaseUrl;
        this.mailFrom = mailFrom;
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

            sendResetEmail(customer.getEmail(), resetToken.getToken());
        });

        return GENERIC_RESET_MESSAGE;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (!PasswordPolicy.isValid(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PasswordPolicy.MESSAGE);
        }

        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token)
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is invalid or has expired."));

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

    private void sendResetEmail(String email, String token) {
        if (mailSender == null) {
            logger.warn("Password reset email could not be sent because mail is not configured");
            return;
        }

        String resetUrl = appBaseUrl + "/reset-password?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Reset your Pete's Pizza password");
        message.setText("""
                A password reset was requested for your Pete's Pizza account.

                Use this link within 30 minutes:
                %s

                If you did not request this, you can ignore this email.
                """.formatted(resetUrl));
        mailSender.send(message);
    }
}
