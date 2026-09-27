package org.example.onlinepossystem.customer.api;

/** Published after a password change so long-lived authenticated channels close. */
public record AccountCredentialsChanged(String username) {}
