package org.example.onlinepossystem.customer.notification;

public interface PasswordResetNotifier {
    void sendResetLink(String email, String token);
}
