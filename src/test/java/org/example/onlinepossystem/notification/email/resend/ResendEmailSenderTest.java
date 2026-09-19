package org.example.onlinepossystem.notification.email.resend;

import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.example.onlinepossystem.notification.email.EmailMessage;
import org.example.onlinepossystem.notification.email.NotificationDeliveryException;
import org.example.onlinepossystem.notification.email.NotificationDeliveryException.Reason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class ResendEmailSenderTest {
    private final ResendClient resendClient = mock(ResendClient.class);
    private final ResendProperties properties = configuredProperties();
    private final ResendEmailSender sender = new ResendEmailSender(resendClient, properties);
    private final EmailMessage message = new EmailMessage(
            "customer@example.com", "Reset your password", "<p>private-reset-token</p>", "private-reset-token");

    @Test
    void buildsProviderRequestWithoutDeliveringRealEmail(CapturedOutput output) throws Exception {
        when(resendClient.send(any(CreateEmailOptions.class)))
                .thenReturn(new CreateEmailResponse("email_123"));

        sender.send(message);

        ArgumentCaptor<CreateEmailOptions> request = ArgumentCaptor.forClass(CreateEmailOptions.class);
        verify(resendClient).send(request.capture());
        assertThat(request.getValue().getFrom()).isEqualTo("noreply@email.crowdcam.co.za");
        assertThat(request.getValue().getTo()).containsExactly("customer@example.com");
        assertThat(request.getValue().getSubject()).isEqualTo("Reset your password");
        assertThat(request.getValue().getHtml()).isEqualTo("<p>private-reset-token</p>");
        assertThat(request.getValue().getText()).isEqualTo("private-reset-token");
        assertThat(output).contains("Resend accepted email", "email_123")
                .doesNotContain("private-reset-token", properties.getApiKey(), message.recipient());
    }

    @Test
    void missingKeyStopsBeforeSdkCall(CapturedOutput output) {
        properties.setApiKey(" ");
        assertFailure(Reason.MISSING_API_KEY);
        verifyNoInteractions(resendClient);
        assertThat(output).contains("set RESEND_API_KEY");
    }

    @Test
    void missingSenderStopsBeforeSdkCall() {
        properties.setFromEmail("");
        assertFailure(Reason.MISSING_SENDER);
        verifyNoInteractions(resendClient);
    }

    @ParameterizedTest
    @CsvSource({
            "401,invalid_api_key,Invalid API key,INVALID_API_KEY",
            "403,validation_error,The sender domain is not verified,SENDER_VERIFICATION",
            "429,rate_limit_exceeded,Too many requests,RATE_LIMITED",
            "403,daily_quota_exceeded,Quota reached,RATE_LIMITED",
            "401,restricted_api_key,This API key is restricted to only send emails,PERMISSION_DENIED",
            "500,application_error,Provider failed,PROVIDER_ERROR"
    })
    void classifiesProviderFailuresWithoutExposingRawResponse(int status, String name, String detail,
                                                            Reason expected, CapturedOutput output) throws Exception {
        ResendException failure = new ResendException(status,
                "{\"name\":\"" + name + "\",\"message\":\"" + detail + " re_test_key\"}");
        when(resendClient.send(any(CreateEmailOptions.class))).thenThrow(failure);
        assertThatThrownBy(() -> sender.send(message)).isInstanceOfSatisfying(NotificationDeliveryException.class, ex -> {
            assertThat(ex.getReason()).isEqualTo(expected);
            StringWriter trace = new StringWriter();
            ex.printStackTrace(new PrintWriter(trace));
            assertThat(trace.toString()).doesNotContain("re_test_key");
        });
        assertThat(output).contains("reason=" + expected).doesNotContain("re_test_key");
    }

    @Test
    void wrapsNetworkFailureForResetServiceTokenCleanup() throws Exception {
        when(resendClient.send(any(CreateEmailOptions.class)))
                .thenThrow(new RuntimeException(new IOException("Network failed re_test_key")));
        assertFailure(Reason.NETWORK_ERROR);
    }

    @Test
    void rejectsEmptyProviderResponse() throws Exception {
        when(resendClient.send(any(CreateEmailOptions.class))).thenReturn(null, new CreateEmailResponse());
        assertFailure(Reason.PROVIDER_ERROR);
        assertFailure(Reason.PROVIDER_ERROR);
    }

    private void assertFailure(Reason expected) {
        assertThatThrownBy(() -> sender.send(message)).isInstanceOfSatisfying(NotificationDeliveryException.class,
                ex -> assertThat(ex.getReason()).isEqualTo(expected));
    }

    private static ResendProperties configuredProperties() {
        ResendProperties properties = new ResendProperties();
        properties.setApiKey("re_test_key");
        properties.setFromEmail("noreply@email.crowdcam.co.za");
        return properties;
    }
}
