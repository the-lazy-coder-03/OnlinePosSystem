package org.example.onlinepossystem.security.rls;

import org.example.onlinepossystem.security.api.AccountPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Runs only on the connection whose JPA transaction has just begun. */
public final class RlsContextInitializer {
    private static final Logger logger = LoggerFactory.getLogger(RlsContextInitializer.class);

    public void initialize(Connection connection) throws SQLException {
        if (connection.getAutoCommit()) {
            throw new SQLException("RLS context requires an active transaction", "25000");
        }
        String customerId = "";
        String branchId = "";
        String role = "";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            if (principal.environmentAdmin() && principal.customerId() == null && principal.accessLevel() == 3) {
                role = "SUPER_ADMIN";
            } else if (principal.customerId() != null && principal.customerId() > 0) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT access_level FROM app_security.account_context(?)")) {
                    statement.setLong(1, principal.customerId());
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            int level = result.getInt(1);
                            if (level >= 0 && level <= 4) {
                                customerId = principal.customerId().toString();
                                role = switch (level) {
                                    case 1, 2 -> "ADMIN";
                                    case 3 -> "SUPER_ADMIN";
                                    case 4 -> "DRIVER";
                                    default -> "USER";
                                };
                                if (level == 1 || level == 2) branchId = Integer.toString(level);
                            }
                        }
                    }
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT set_config('app.user_id', ?, true), set_config('app.customer_id', ?, true),
                       set_config('app.branch_id', ?, true), set_config('app.role', ?, true)
                """)) {
            statement.setString(1, customerId);
            statement.setString(2, customerId);
            statement.setString(3, branchId);
            statement.setString(4, role);
            statement.execute();
        }
        logger.debug("Initialized transaction-local RLS context; role={}", role.isEmpty() ? "anonymous" : role);
    }
}
