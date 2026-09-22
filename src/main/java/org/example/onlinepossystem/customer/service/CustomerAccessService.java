package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.api.AccountAccess;
import org.example.onlinepossystem.customer.api.AccountAccessAdministration;
import org.example.onlinepossystem.customer.api.AccountAccessReader;
import org.example.onlinepossystem.customer.api.AccountAccessSummary;
import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CustomerAccessService implements AccountAccessReader, AccountAccessAdministration {
    private static final int SUPER_ADMIN_LEVEL = 3;

    private final CustomerRepository customerRepository;
    private final String environmentAdminUsername;
    private final org.springframework.context.ApplicationEventPublisher events;

    public CustomerAccessService(CustomerRepository customerRepository,
                                 @Value("${ADMIN_USERNAME}") String environmentAdminUsername,
                                 org.springframework.context.ApplicationEventPublisher events) {
        this.customerRepository = customerRepository;
        this.environmentAdminUsername = environmentAdminUsername;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountAccess findByUsername(String username) {
        if (username != null && username.equals(environmentAdminUsername)) {
            return new AccountAccess(SUPER_ADMIN_LEVEL);
        }
        if (username == null || username.isBlank()) {
            return new AccountAccess(0);
        }
        return customerRepository.findByEmail(username)
                .or(() -> customerRepository.findByPhone1(username))
                .or(() -> customerRepository.findByPhone2(username))
                .map(customer -> new AccountAccess(effectiveLevel(customer)))
                .orElseGet(() -> new AccountAccess(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountAccessSummary> search(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return customerRepository.findAll().stream()
                .filter(customer -> !customer.isEnvironmentAdmin())
                .filter(customer -> needle.isEmpty() || searchableText(customer).contains(needle))
                .sorted(Comparator.comparing(Customer::getEmail, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .limit(50)
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public void assignAccessLevel(Long customerId, int accessLevel) {
        AccountAccess validated = new AccountAccess(accessLevel);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Customer not found with ID: " + customerId));
        if (customer.isEnvironmentAdmin()) {
            throw new java.util.NoSuchElementException("Customer not found with ID: " + customerId);
        }
        customer.setAccessLevel(validated.level());
        customer.setRole(roleFor(validated.level()));
        customerRepository.save(customer);
        events.publishEvent(new org.example.onlinepossystem.customer.api.AccountAccessChanged(customer.getId(), customer.getEmail()));
    }

    private AccountAccessSummary toSummary(Customer customer) {
        int level = effectiveLevel(customer);
        return new AccountAccessSummary(
                customer.getId(),
                displayName(customer),
                customer.getEmail(),
                customer.getPhone1(),
                level,
                labelFor(level)
        );
    }

    private int effectiveLevel(Customer customer) {
        if (customer.getAccessLevel() != null) {
            return customer.getAccessLevel();
        }
        String role = customer.getRole() == null ? "" : customer.getRole().trim().toUpperCase(Locale.ROOT);
        return switch (role.replace("ROLE_", "")) {
            case "ADMIN", "SUPER_ADMIN" -> 3;
            case "DRIVER" -> 4;
            default -> 0;
        };
    }

    private String roleFor(int level) {
        return switch (level) {
            case 1, 2 -> "ADMIN";
            case 3 -> "SUPER_ADMIN";
            case 4 -> "DRIVER";
            default -> "USER";
        };
    }

    private String labelFor(int level) {
        return switch (level) {
            case 1 -> "Kenridge admin";
            case 2 -> "Uitzicht admin";
            case 3 -> "Super admin";
            case 4 -> "Delivery driver";
            default -> "Normal user";
        };
    }

    private String searchableText(Customer customer) {
        return String.join(" ",
                safe(customer.getFirstName()),
                safe(customer.getLastName()),
                safe(customer.getEmail()),
                safe(customer.getPhone1()),
                safe(customer.getPhone2())
        ).toLowerCase(Locale.ROOT);
    }

    private String displayName(Customer customer) {
        String name = (safe(customer.getFirstName()) + " " + safe(customer.getLastName())).trim();
        return name.isEmpty() ? "Unnamed account" : name;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
