package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.api.EnvironmentAdminAccount;
import org.example.onlinepossystem.security.api.AccountPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentAdminAccountService implements EnvironmentAdminAccount {
    private final JdbcTemplate jdbc;

    public EnvironmentAdminAccountService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public Long ensureCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)
                || !principal.environmentAdmin() || principal.accessLevel() != 3) {
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
