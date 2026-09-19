package org.example.onlinepossystem.customer.notification;

import org.example.onlinepossystem.notification.email.EmailMessage;
import org.example.onlinepossystem.notification.email.EmailSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PasswordResetNotificationServiceTest {
    @Test
    void resetLinkUsesEmailSubdomain() {
        EmailSender emailSender = mock(EmailSender.class);
        PasswordResetNotificationService service = new PasswordResetNotificationService(
                emailSender,
                "https://email.crowdcam.co.za/"
        );

        service.sendResetLink("customer@example.com", "raw-token");

        ArgumentCaptor<EmailMessage> message = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(message.capture());
        assertThat(message.getValue().textContent())
                .contains("https://email.crowdcam.co.za/reset-password?token=raw-token");
    }
}
