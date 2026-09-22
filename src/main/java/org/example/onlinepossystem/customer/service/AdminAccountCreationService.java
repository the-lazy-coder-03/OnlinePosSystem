package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.api.AdminAccountCreator;
import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.example.onlinepossystem.security.api.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AdminAccountCreationService implements AdminAccountCreator {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public AdminAccountCreationService(CustomerRepository customers, PasswordEncoder passwordEncoder,
                                       PasswordPolicy passwordPolicy) {
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    @Override
    @Transactional
    public Long create(String firstName, String lastName, String email, String password, int accessLevel) {
        if (accessLevel < 1 || accessLevel > 3) {
            throw new IllegalArgumentException("Admin access level must be between 1 and 3.");
        }
        String givenName = requiredName(firstName);
        String familyName = requiredName(lastName);
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.length() > 255 || !EMAIL.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (!passwordPolicy.isValid(password)) {
            throw new CustomerRegistrationException(CustomerRegistrationException.Reason.PASSWORD,
                    passwordPolicy.validationMessage());
        }
        if (customers.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new CustomerRegistrationException(CustomerRegistrationException.Reason.EMAIL,
                    "Email is already registered.");
        }

        Customer account = new Customer();
        account.setFirstName(givenName);
        account.setLastName(familyName);
        account.setEmail(normalizedEmail);
        account.setPassword(passwordEncoder.encode(password));
        account.setAccessLevel(accessLevel);
        account.setRole(accessLevel == 3 ? "SUPER_ADMIN" : "ADMIN");
        return customers.saveAndFlush(account).getId();
    }

    private String requiredName(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || trimmed.length() > 255) {
            throw new IllegalArgumentException("Enter a name of 1 to 255 characters.");
        }
        return trimmed;
    }
}
