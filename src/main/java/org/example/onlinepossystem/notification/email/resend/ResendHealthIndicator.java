package org.example.onlinepossystem.notification.email.resend;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("resendHealthIndicator")
public class ResendHealthIndicator implements HealthIndicator {
    private final ResendProperties properties;

    public ResendHealthIndicator(ResendProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        boolean keyConfigured = StringUtils.hasText(properties.getApiKey());
        boolean senderConfigured = StringUtils.hasText(properties.getFromEmail());
        // Configuration readiness only; no network calls or automatic test emails.
        return (keyConfigured && senderConfigured ? Health.up() : Health.down())
                .withDetail("check", "configuration-only")
                .withDetail("apiKeyConfigured", keyConfigured)
                .withDetail("senderConfigured", senderConfigured)
                .build();
    }
}
