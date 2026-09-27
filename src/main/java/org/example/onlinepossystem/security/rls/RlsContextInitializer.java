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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Runs only on the connection whose JPA transaction has just begun. */
public class RlsContextInitializer {
    private static final Logger logger = LoggerFactory.getLogger(RlsContextInitializer.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] contextSecret;

    public RlsContextInitializer(String contextSecret) {
        if (contextSecret == null || contextSecret.isBlank() || contextSecret.length() < 32) {
            throw new IllegalStateException("RLS_CONTEXT_SECRET must contain at least 32 characters.");
        }
        this.contextSecret = contextSecret.getBytes(StandardCharsets.UTF_8);
    }

    public void initialize(Connection connection) throws SQLException {
        if (connection.getAutoCommit()) {
            throw new SQLException("RLS context requires an active transaction", "25000");
        }
        Long customerId = null;
        boolean environmentAdmin = false;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            if (principal.environmentAdmin() && principal.customerId() == null && principal.accessLevel() == 3) {
                environmentAdmin = true;
            } else if (principal.customerId() != null && principal.customerId() > 0) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT access_level FROM app_security.account_context(?)")) {
                    statement.setLong(1, principal.customerId());
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            int level = result.getInt(1);
                            if (level >= 0 && level <= 4) {
                                customerId = principal.customerId();
                            }
                        }
                    }
                }
            }
        }
        TransactionIdentity transaction = transactionIdentity(connection);
        String customerSetting = customerId == null ? "" : customerId.toString();
        String environmentAdminSetting = environmentAdmin ? "true" : "";
        String signedSubject = environmentAdmin ? "environment-admin" : customerSetting;
        String signature = signedSubject.isEmpty() ? "" : sign(signedSubject, transaction);
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT set_config('app.customer_id', ?, true), set_config('app.context_signature', ?, true),
                       set_config('app.environment_admin', ?, true),
                       set_config('app.user_id', '', true), set_config('app.branch_id', '', true),
                       set_config('app.role', '', true)
                """)) {
            statement.setString(1, customerSetting);
            statement.setString(2, signature);
            statement.setString(3, environmentAdminSetting);
            statement.execute();
        }
        logger.debug("Initialized signed transaction-local RLS context; authenticated={}",
                customerId != null || environmentAdmin);
    }

    private TransactionIdentity transactionIdentity(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT pg_backend_pid(), pg_current_xact_id()::text")) {
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Cannot resolve PostgreSQL transaction identity", "55000");
                return new TransactionIdentity(result.getLong(1), result.getString(2));
            }
        }
    }

    private String sign(String subject, TransactionIdentity transaction) throws SQLException {
        String payload = "rls-context-v1|" + subject + "|" + transaction.backendPid()
                + "|" + transaction.transactionId();
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(contextSecret, HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new SQLException("Cannot initialize the RLS context signature", "58000", exception);
        }
    }

    private record TransactionIdentity(long backendPid, String transactionId) {}
}
