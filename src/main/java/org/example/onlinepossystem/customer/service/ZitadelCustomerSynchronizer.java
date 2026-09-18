package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;

public interface ZitadelCustomerSynchronizer {
    Customer syncZitadelCustomer(String zitadelSubject, String email, String firstName, String lastName);
}
