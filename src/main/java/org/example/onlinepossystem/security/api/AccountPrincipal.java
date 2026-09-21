package org.example.onlinepossystem.security.api;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/** Authenticated account identity; branch assignments are never supplied by clients. */
public final class AccountPrincipal extends User {
    private final Long customerId;
    private final int accessLevel;
    private final boolean environmentAdmin;

    public AccountPrincipal(String username, String password, Long customerId, int accessLevel,
                            boolean environmentAdmin) {
        super(username, password, authorities(accessLevel));
        this.customerId = customerId;
        this.accessLevel = accessLevel;
        this.environmentAdmin = environmentAdmin;
    }

    public Long customerId() { return customerId; }
    public int accessLevel() { return accessLevel; }
    public boolean environmentAdmin() { return environmentAdmin; }

    private static List<SimpleGrantedAuthority> authorities(int level) {
        return switch (level) {
            case 0 -> List.of(new SimpleGrantedAuthority("ROLE_USER"));
            case 1, 2 -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case 3 -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            case 4 -> List.of(new SimpleGrantedAuthority("ROLE_DRIVER"));
            default -> List.of();
        };
    }
}
