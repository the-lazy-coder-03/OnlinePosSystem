package org.example.onlinepossystem.notification.email.resend;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.example.onlinepossystem.notification.email.EmailMessage;
import org.example.onlinepossystem.notification.email.NotificationDeliveryException;
import org.example.onlinepossystem.notification.email.NotificationDeliveryException.Reason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.skyscreamer.jsonassert.JSONAssert;
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
    private final Emails emails = mock(Emails.class);
    private final ResendProperties properties = configuredProperties();
    private final ResendEmailSender sender = new ResendEmailSender(emails, properties);
    private final EmailMessage message = new EmailMessage(
            "customer@example.com", "Reset your password", "<p>private-reset-token</p>", "private-reset-token");

    @Test
    void sdkSendsOnlyPostEmailsWithBearerAuthAndJson(CapturedOutput output) throws Exception {
        Call call = mock(Call.class);
        when(call.execute()).thenReturn(new Response.Builder()
                .request(new Request.Builder().url("https://api.resend.com/emails").build())
                .protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(ResponseBody.create("{\"id\":\"email_123\"}", MediaType.get("application/json"))).build());
        try (MockedConstruction<OkHttpClient> clients = mockConstruction(OkHttpClient.class,
                (client, context) -> when(client.newCall(any(Request.class))).thenReturn(call))) {
            Emails sdk = new Resend(properties.getApiKey()).emails();
            new ResendEmailSender(sdk, properties).send(message);
            assertThat(clients.constructed()).hasSize(1);
            ArgumentCaptor<Request> request = ArgumentCaptor.forClass(Request.class);
            verify(clients.constructed().get(0)).newCall(request.capture());
            assertThat(request.getValue().url().toString()).isEqualTo("https://api.resend.com/emails");
            assertThat(request.getValue().method()).isEqualTo("POST");
            assertThat(request.getValue().header("Authorization")).isEqualTo("Bearer re_test_key");
            assertThat(request.getValue().body().contentType().toString()).startsWith("application/json");
            Buffer buffer = new Buffer();
            request.getValue().body().writeTo(buffer);
            JSONAssert.assertEquals("""
                    {"from":"noreply@email.crowdcam.co.za","to":["customer@example.com"],
                     "subject":"Reset your password","html":"<p>private-reset-token</p>",
                     "text":"private-reset-token"}
                    """, buffer.readUtf8(), true);
        }
        assertThat(output).contains("Resend accepted email", "email_123")
                .doesNotContain("private-reset-token", properties.getApiKey(), message.recipient());
    }

    @Test
    void missingKeyStopsBeforeSdkCall(CapturedOutput output) {
        properties.setApiKey(" ");
        assertFailure(Reason.MISSING_API_KEY);
        verifyNoInteractions(emails);
        assertThat(output).contains("set RESEND_API_KEY");
    }

    @Test
    void missingSenderStopsBeforeSdkCall() {
        properties.setFromEmail("");
        assertFailure(Reason.MISSING_SENDER);
        verifyNoInteractions(emails);
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
        when(emails.send(any(CreateEmailOptions.class))).thenThrow(failure);
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
        when(emails.send(any(CreateEmailOptions.class)))
                .thenThrow(new RuntimeException(new IOException("Network failed re_test_key")));
        assertFailure(Reason.NETWORK_ERROR);
    }

    @Test
    void rejectsEmptyProviderResponse() throws Exception {
        when(emails.send(any(CreateEmailOptions.class))).thenReturn(null, new CreateEmailResponse());
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
