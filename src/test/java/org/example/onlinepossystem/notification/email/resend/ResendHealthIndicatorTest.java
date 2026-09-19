package org.example.onlinepossystem.notification.email.resend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class ResendHealthIndicatorTest {
    @Test
    void checksConfigurationWithoutProviderRequestsOrTestEmails() {
        ResendProperties properties = new ResendProperties();
        properties.setApiKey("re_sending_only_test");
        properties.setFromEmail("noreply@email.crowdcam.co.za");
        ResendHealthIndicator health = new ResendHealthIndicator(properties);
        assertThat(health.health().getStatus()).isEqualTo(Status.UP);
        assertThat(health.health().getDetails()).containsEntry("check", "configuration-only");
        assertThat(health.health().toString()).doesNotContain(properties.getApiKey());
    }

    @Test
    void reportsMissingConfigurationWithoutSecrets() {
        ResendProperties properties = new ResendProperties();
        ResendHealthIndicator health = new ResendHealthIndicator(properties);
        assertThat(health.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.health().getDetails()).containsEntry("apiKeyConfigured", false)
                .containsEntry("senderConfigured", false);
        properties.setApiKey("re_test");
        assertThat(health.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.health().getDetails()).containsEntry("apiKeyConfigured", true);
    }
}
