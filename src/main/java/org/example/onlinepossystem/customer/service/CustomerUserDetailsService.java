package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.persistence.AccountBootstrapStore;
import org.example.onlinepossystem.security.api.AccountPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerUserDetailsService implements UserDetailsService {
    private final AccountBootstrapStore accounts;
    private final String adminUsername;
    private final String adminPassword;

    public CustomerUserDetailsService(AccountBootstrapStore accounts,
            @Value("${ADMIN_USERNAME}") String adminUsername,
            @Value("${ADMIN_PASSWORD}") String adminPassword) {
        this.accounts = accounts;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) {
        if (identifier == null || identifier.isBlank()) throw new UsernameNotFoundException("Invalid account");
        String normalized = identifier.trim();
        if (normalized.equals(adminUsername)) {
            return new AccountPrincipal(adminUsername, "{noop}" + adminPassword, null, 3, true);
        }
        var account = accounts.findCredentials(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid account"));
        String password = account.password();
        if (password == null || !password.startsWith("{")) password = "{bcrypt}" + password;
        return new AccountPrincipal(account.email(), password, account.id(), account.accessLevel(), false);
    }
}
