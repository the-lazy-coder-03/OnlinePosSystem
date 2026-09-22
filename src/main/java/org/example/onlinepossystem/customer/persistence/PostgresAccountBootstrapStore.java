package org.example.onlinepossystem.customer.persistence;

import org.example.onlinepossystem.customer.entity.Customer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@Transactional
@ConditionalOnProperty(name = "app.rls.enabled", havingValue = "true", matchIfMissing = true)
public class PostgresAccountBootstrapStore implements AccountBootstrapStore {
    private final JdbcTemplate jdbc;

    public PostgresAccountBootstrapStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional(readOnly = true)
    public Optional<Credentials> findCredentials(String identifier) {
        return jdbc.query("SELECT * FROM app_security.login_credentials(?)",
                (rs, row) -> new Credentials(rs.getLong("id"), rs.getString("email"),
                        rs.getString("password"), rs.getInt("access_level")), identifier).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT app_security.account_exists(?, NULL)", Boolean.class, email));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean phoneExists(String phone) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT app_security.account_exists(NULL, ?)", Boolean.class, phone));
    }

    @Override
    public Customer register(Customer c) {
        Long id = jdbc.queryForObject("SELECT app_security.register_customer(?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Long.class,
                c.getFirstName(), c.getLastName(), c.getEmail(), c.getPassword(), c.getPhone1(), c.getPhone2(),
                c.getHouseNumber(), c.getStreet(), c.getArea(), c.getComplexName(), c.getPreferredStore(),
                c.getPostalCode(), c.getCity(), c.getLastOrderedAt());
        c.setId(id);
        c.setAccessLevel(0);
        c.setRole("USER");
        return c;
    }

    @Override
    public Optional<String> createReset(String email, String tokenHash, LocalDateTime expiresAt) {
        return Optional.ofNullable(jdbc.queryForObject("SELECT app_security.create_reset(?, ?, ?)",
                String.class, email, tokenHash, expiresAt));
    }

    @Override
    public void cancelReset(String tokenHash) {
        jdbc.queryForObject("SELECT app_security.cancel_reset(?)", Object.class, tokenHash);
    }

    @Override
    public boolean consumeReset(String tokenHash, String passwordHash) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT app_security.consume_reset(?, ?)",
                Boolean.class, tokenHash, passwordHash));
    }
}
