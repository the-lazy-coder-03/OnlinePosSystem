package org.example.onlinepossystem.customer.persistence;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.entity.PasswordResetToken;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.example.onlinepossystem.customer.repository.PasswordResetTokenRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/** H2-only adapter. PostgreSQL runtime startup rejects disabled RLS. */
@Repository
@Transactional
@ConditionalOnProperty(name = "app.rls.enabled", havingValue = "false")
public class JpaAccountBootstrapStore implements AccountBootstrapStore {
    private final CustomerRepository customers;
    private final PasswordResetTokenRepository tokens;

    public JpaAccountBootstrapStore(CustomerRepository customers, PasswordResetTokenRepository tokens) {
        this.customers = customers;
        this.tokens = tokens;
    }

    @Override
    public Optional<Credentials> findCredentials(String identifier) {
        return customers.findByEmail(identifier).or(() -> customers.findByPhone1(identifier))
                .or(() -> customers.findByPhone2(identifier))
                .map(c -> new Credentials(c.getId(), c.getEmail(), c.getPassword(), effectiveLevel(c)));
    }

    private int effectiveLevel(Customer c) {
        if (c.getAccessLevel() != null) return c.getAccessLevel();
        return switch (c.getRole() == null ? "" : c.getRole().replace("ROLE_", "").trim().toUpperCase(java.util.Locale.ROOT)) {
            case "ADMIN", "SUPER_ADMIN" -> 3;
            case "DRIVER" -> 4;
            default -> 0;
        };
    }

    @Override public boolean emailExists(String email) { return customers.findByEmail(email).isPresent(); }
    @Override public boolean phoneExists(String phone) { return customers.findByPhone1(phone).isPresent(); }
    @Override public Customer register(Customer customer) { return customers.save(customer); }

    @Override
    public Optional<String> createReset(String email, String hash, LocalDateTime expiresAt) {
        return customers.findByEmail(email).map(customer -> {
            tokens.deleteByCustomerAndUsedFalse(customer);
            PasswordResetToken token = new PasswordResetToken();
            token.setCustomer(customer);
            token.setTokenHash(hash);
            token.setExpiresAt(expiresAt);
            tokens.save(token);
            return customer.getEmail();
        });
    }

    @Override
    public void cancelReset(String hash) {
        tokens.findByTokenHashAndUsedFalse(hash).ifPresent(tokens::delete);
    }

    @Override
    public boolean consumeReset(String hash, String password) {
        return tokens.findByTokenHashAndUsedFalse(hash).filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(token -> {
                    token.getCustomer().setPassword(password);
                    customers.save(token.getCustomer());
                    token.setUsed(true);
                    tokens.save(token);
                    tokens.deleteByCustomerAndUsedFalse(token.getCustomer());
                    return true;
                }).orElse(false);
    }
}
