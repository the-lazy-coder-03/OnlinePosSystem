package org.example.onlinepossystem.customer.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailPasswordResetNotifier implements PasswordResetNotifier {
    private static final Logger logger = LoggerFactory.getLogger(EmailPasswordResetNotifier.class);

    private final JavaMailSender mailSender;
    private final String appBaseUrl;
    private final String mailFrom;

    public EmailPasswordResetNotifier(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.base-url:http://localhost:8080}") String appBaseUrl,
            @Value("${spring.mail.username:no-reply@localhost}") String mailFrom
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.appBaseUrl = appBaseUrl;
        this.mailFrom = mailFrom;
    }

    @Override
    public void sendResetLink(String email, String token) {
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
