package org.example.onlinepossystem.customer.service;

public class CustomerRegistrationException extends IllegalArgumentException {
    private final Reason reason;

    public CustomerRegistrationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        PASSWORD,
        EMAIL,
        PHONE
    }
}
