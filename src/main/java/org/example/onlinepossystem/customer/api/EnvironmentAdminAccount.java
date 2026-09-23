package org.example.onlinepossystem.customer.api;

public interface EnvironmentAdminAccount {
    boolean matches(Object principal);

    Long ensureCustomerId(Object principal);
}
