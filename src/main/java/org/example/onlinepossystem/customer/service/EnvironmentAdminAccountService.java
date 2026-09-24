package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.api.EnvironmentAdminAccount;
import org.example.onlinepossystem.security.api.AccountPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentAdminAccountService implements EnvironmentAdminAccount {
    private final JdbcTemplate jdbc;

    public EnvironmentAdminAccountService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean matches(Object principal) {
        return principal instanceof AccountPrincipal account
                && account.environmentAdmin()
                && account.accessLevel() == 3;
    }

    @Override
    @Transactional
    public Long ensureCustomerId(Object principal) {
        if (!matches(principal)) {
            throw new AccessDeniedException("Environment admin authentication is required.");
        }
        jdbc.update("""
                INSERT INTO public.customers (first_name, last_name, role, access_level, environment_admin)
                VALUES ('System', 'Admin', 'SUPER_ADMIN', 3, true)
                ON CONFLICT DO NOTHING
                """);
        return jdbc.queryForObject(
                "SELECT id FROM public.customers WHERE environment_admin = true", Long.class);
    }
}
