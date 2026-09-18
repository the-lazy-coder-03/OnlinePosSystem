package org.example.onlinepossystem.notification.email.resend;

import org.example.onlinepossystem.notification.email.EmailMessage;
import org.example.onlinepossystem.notification.email.NotificationDeliveryException;
import org.example.onlinepossystem.shared.config.HttpJsonClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.POST;

class ResendEmailSenderTest {

    @Test
    void sendsProviderNeutralEmailThroughResendRequest() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ResendProperties properties = new ResendProperties();
        properties.setApiKey("re_test_key");
        properties.setFromEmail("noreply@example.com");
        ResendEmailSender sender = new ResendEmailSender(new HttpJsonClient(restTemplate), properties);

        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(POST))
                .andExpect(header(AUTHORIZATION, "Bearer re_test_key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "from": "noreply@example.com",
                          "to": ["customer@example.com"],
                          "subject": "Reset your password",
                          "html": "<p>Reset</p>",
                          "text": "Reset"
                        }
                        """))
                .andRespond(withSuccess("{\"id\":\"email_123\"}", MediaType.APPLICATION_JSON));

        sender.send(new EmailMessage(
                "customer@example.com",
                "Reset your password",
                "<p>Reset</p>",
                "Reset"
        ));

        server.verify();
    }

    @Test
    void failsBeforeHttpCallWhenApiKeyIsMissing() {
        ResendProperties properties = new ResendProperties();
        properties.setApiKey("");
        ResendEmailSender sender = new ResendEmailSender(new HttpJsonClient(new RestTemplate()), properties);

        assertThatThrownBy(() -> sender.send(new EmailMessage(
                "customer@example.com",
                "Reset your password",
                "<p>Reset</p>",
                "Reset"
        )))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessageContaining("not configured");
    }
}
