package org.example.onlinepossystem.notification.email.resend;

import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.example.onlinepossystem.notification.email.EmailMessage;
import org.example.onlinepossystem.notification.email.EmailSender;
import org.example.onlinepossystem.notification.email.NotificationDeliveryException;
import org.example.onlinepossystem.notification.email.NotificationDeliveryException.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Locale;

@Service
public class ResendEmailSender implements EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(ResendEmailSender.class);

    private final ResendClient resendClient;
    private final ResendProperties properties;

    public ResendEmailSender(ResendClient resendClient, ResendProperties properties) {
        this.resendClient = resendClient;
        this.properties = properties;
    }

    @Override
    public void send(EmailMessage message) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            logger.warn("Resend email delivery is not configured: set RESEND_API_KEY.");
            throw new NotificationDeliveryException(Reason.MISSING_API_KEY);
        }
        if (!StringUtils.hasText(properties.getFromEmail())) {
            logger.warn("Resend email delivery is not configured: set RESEND_FROM_EMAIL.");
            throw new NotificationDeliveryException(Reason.MISSING_SENDER);
        }

        CreateEmailOptions request = CreateEmailOptions.builder()
                .from(properties.getFromEmail())
                .to(message.recipient())
                .subject(message.subject())
                .html(message.htmlContent())
                .text(message.textContent())
                .build();

        try {
            CreateEmailResponse response = resendClient.send(request);
            if (response == null || !StringUtils.hasText(response.getId())) {
                logger.warn("Resend returned no email ID for the email request.");
                throw new NotificationDeliveryException(Reason.PROVIDER_ERROR);
            }
            logger.info("Resend accepted email; email ID {}", response.getId());
        } catch (ResendException ex) {
            Reason reason = classify(ex);
            logger.warn("Resend rejected email: status={}, reason={}", ex.getStatusCode(), reason);
            // Do not propagate raw provider bodies, which can contain request data.
            throw new NotificationDeliveryException(reason);
        } catch (NotificationDeliveryException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            // The SDK wraps transport failures in RuntimeException. Preserve the
            // service's generic response and failed-token cleanup in this case too.
            Reason reason = hasNetworkCause(ex) ? Reason.NETWORK_ERROR : Reason.PROVIDER_ERROR;
            logger.warn("Resend email request failed: reason={}", reason);
            throw new NotificationDeliveryException(reason);
        }
    }

    private Reason classify(ResendException ex) {
        String name = ex.getErrorName() == null ? "" : ex.getErrorName();
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
        if (Integer.valueOf(429).equals(ex.getStatusCode()) || name.endsWith("quota_exceeded")) {
            return Reason.RATE_LIMITED;
        }
        if (name.equals("restricted_api_key") || name.equals("invalid_permission") || name.equals("suspended_api_key")) {
            return Reason.PERMISSION_DENIED;
        }
        if (Integer.valueOf(401).equals(ex.getStatusCode()) || name.equals("invalid_api_key")) {
            return Reason.INVALID_API_KEY;
        }
        if (Integer.valueOf(403).equals(ex.getStatusCode()) && name.equals("validation_error")
                && (message.contains("domain") || message.contains("sender") || message.contains("testing emails"))) {
            return Reason.SENDER_VERIFICATION;
        }
        return Reason.PROVIDER_ERROR;
    }

    private boolean hasNetworkCause(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof IOException) {
                return true;
            }
        }
        return false;
    }
}
