package org.example.onlinepossystem.customer.notification;

import org.example.onlinepossystem.notification.email.EmailMessage;
import org.example.onlinepossystem.notification.email.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetNotificationService implements PasswordResetNotifier {

    private final EmailSender emailSender;
    private final String appBaseUrl;

    public PasswordResetNotificationService(
            EmailSender emailSender,
            @Value("${app.base-url:http://localhost:8080}") String appBaseUrl
    ) {
        this.emailSender = emailSender;
        this.appBaseUrl = normalizeBaseUrl(appBaseUrl);
    }

    @Override
    public void sendResetLink(String email, String token) {
        String resetUrl = UriComponentsBuilder.fromUriString(appBaseUrl)
                .path("/reset-password")
                .queryParam("token", token)
                .build()
                .toUriString();

        emailSender.send(new EmailMessage(
                email,
                "Reset your Pete's Pizza password",
                htmlContent(resetUrl),
                textContent(resetUrl)
        ));
    }

    private String htmlContent(String resetUrl) {
        String safeResetUrl = HtmlUtils.htmlEscape(resetUrl);
        return """
                <p>A password reset was requested for your Pete's Pizza account.</p>
                <p><a href="%s" style="display:inline-block;padding:12px 18px;background:#0052cc;color:#ffffff;text-decoration:none;border-radius:6px;">Reset Password</a></p>
                <p>This link expires in 30 minutes.</p>
                <p>If you did not request this password reset, you can ignore this email.</p>
                """.formatted(safeResetUrl);
    }

    private String textContent(String resetUrl) {
        return """
                A password reset was requested for your Pete's Pizza account.

                Use this link within 30 minutes:
                %s

                If you did not request this password reset, you can ignore this email.
                """.formatted(resetUrl);
    }

    private String normalizeBaseUrl(String value) {
        String fallback = "http://localhost:8080";
        String normalized = StringUtils.hasText(value) ? value.trim() : fallback;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
