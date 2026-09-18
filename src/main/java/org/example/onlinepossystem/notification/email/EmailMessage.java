package org.example.onlinepossystem.notification.email;

import org.springframework.util.StringUtils;

public record EmailMessage(
        String recipient,
        String subject,
        String htmlContent,
        String textContent
) {
    public EmailMessage {
        if (!StringUtils.hasText(recipient)) {
            throw new IllegalArgumentException("Email recipient is required.");
        }
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("Email subject is required.");
        }
        if (!StringUtils.hasText(htmlContent) && !StringUtils.hasText(textContent)) {
            throw new IllegalArgumentException("Email content is required.");
        }
    }
}
