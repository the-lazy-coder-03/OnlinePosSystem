package org.example.onlinepossystem.notification.email.resend;

import org.example.onlinepossystem.notification.email.EmailMessage;
import org.example.onlinepossystem.notification.email.EmailSender;
import org.example.onlinepossystem.notification.email.NotificationDeliveryException;
import org.example.onlinepossystem.shared.config.HttpJsonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResendEmailSender implements EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(ResendEmailSender.class);

    private final HttpJsonClient httpJsonClient;
    private final ResendProperties properties;

    public ResendEmailSender(HttpJsonClient httpJsonClient, ResendProperties properties) {
        this.httpJsonClient = httpJsonClient;
        this.properties = properties;
    }

    @Override
    public void send(EmailMessage message) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new NotificationDeliveryException("Email delivery is not configured.");
        }
        if (!StringUtils.hasText(properties.getFromEmail())) {
            throw new NotificationDeliveryException("Email sender address is not configured.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("from", properties.getFromEmail());
        request.put("to", List.of(message.recipient()));
        request.put("subject", message.subject());
        request.put("html", message.htmlContent());
        request.put("text", message.textContent());

        try {
            httpJsonClient.post(properties.getEndpoint(), request, headers);
        } catch (RestClientException ex) {
            logger.warn("Email delivery provider rejected or failed a password reset email request: {}", ex.getMessage());
            throw new NotificationDeliveryException("Password reset email could not be sent.", ex);
        }
    }
}
