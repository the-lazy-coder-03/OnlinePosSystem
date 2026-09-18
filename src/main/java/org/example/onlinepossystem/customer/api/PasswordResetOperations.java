package org.example.onlinepossystem.customer.api;

public interface PasswordResetOperations {
    String requestReset(String email, String clientIp);

    void resetPassword(String token, String newPassword);

    void resetPassword(String token, String newPassword, String confirmPassword);
}
