package org.example.onlinepossystem.security.api;

public interface PasswordPolicy {
    boolean isValid(String password);

    String validationMessage();
}
