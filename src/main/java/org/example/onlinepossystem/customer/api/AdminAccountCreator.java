package org.example.onlinepossystem.customer.api;

public interface AdminAccountCreator {
    Long create(String firstName, String lastName, String email, String password, int accessLevel);
}
