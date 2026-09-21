package org.example.onlinepossystem.customer.api;

public interface AccountAccessReader {
    AccountAccess findByUsername(String username);
}
