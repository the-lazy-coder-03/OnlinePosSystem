package org.example.onlinepossystem.notification.email;

public class NotificationDeliveryException extends RuntimeException {
    public enum Reason {
        MISSING_API_KEY, MISSING_SENDER, INVALID_API_KEY, PERMISSION_DENIED,
        SENDER_VERIFICATION, RATE_LIMITED, NETWORK_ERROR, PROVIDER_ERROR
    }

    private final Reason reason;

    public NotificationDeliveryException(Reason reason) {
        super(switch (reason) {
            case MISSING_API_KEY -> "Email delivery API key is not configured.";
            case MISSING_SENDER -> "Email sender address is not configured.";
            case INVALID_API_KEY -> "Email provider authentication failed.";
            case PERMISSION_DENIED -> "Email provider denied permission to send.";
            case SENDER_VERIFICATION -> "Email sender or domain requires verification.";
            case RATE_LIMITED -> "Email provider rate or quota limit reached.";
            case NETWORK_ERROR -> "Email provider could not be reached.";
            case PROVIDER_ERROR -> "Email provider could not send the message.";
        });
        this.reason = reason;
    }

    public NotificationDeliveryException(String message) {
        super(message);
        this.reason = Reason.PROVIDER_ERROR;
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
        this.reason = Reason.PROVIDER_ERROR;
    }

    public Reason getReason() {
        return reason;
    }
}
