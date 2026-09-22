package org.example.onlinepossystem.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.Locale;

@Component
@Profile("migrate")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MigrationSqlRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(MigrationSqlRunner.class);
    private static final long MIGRATION_LOCK_ID = 721946110L;
    private static final String CATALOG_MIGRATION = "migration.sql";
    private static final String RLS_MIGRATION = "db/rls-v1.sql";

    private final DataSource dataSource;
    private final boolean catalogEnabled;
    private final String runtimeRole;

    public MigrationSqlRunner(
            DataSource dataSource,
            @Value("${app.database.migration.enabled:true}") boolean catalogEnabled,
            @Value("${app.rls.runtime-role:pos_runtime}") String runtimeRole
    ) {
        this.dataSource = dataSource;
        this.catalogEnabled = catalogEnabled;
        this.runtimeRole = requireText(runtimeRole, "Runtime database username is required.");
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            requirePostgres(connection);
            acquireMigrationLock(connection);
            try {
                ensureStateTable(connection);
                if (catalogEnabled) {
                    applyCatalogMigration(connection);
                } else {
                    logger.info("{} startup runner is disabled.", CATALOG_MIGRATION);
                }
                applyRlsMigration(connection);
            } finally {
                rollbackOpenTransaction(connection);
                releaseMigrationLock(connection);
            }
        }
    }

    private void applyCatalogMigration(Connection connection) throws Exception {
        applyMigration(connection, CATALOG_MIGRATION, false, () -> {});
    }

    private void applyRlsMigration(Connection connection) throws Exception {
        applyMigration(connection, RLS_MIGRATION, true, () -> setRuntimeRole(connection));
    }

    private void applyMigration(
            Connection connection,
            String resource,
            boolean immutable,
            SqlPreparation preparation
    ) throws Exception {
        String sql = readResource(resource);
        String checksum = sha256(sql);
        String previousChecksum = findLastAppliedChecksum(connection, resource);
        if (checksum.equals(previousChecksum)) {
            logger.info("{} already applied for checksum {}.", resource, shortChecksum(checksum));
            return;
        }

        if (immutable && previousChecksum != null) {
            throw new IllegalStateException("An applied immutable migration was changed: " + resource);
        }
        boolean originalAutoCommit = connection.getAutoCommit();
        if (immutable) connection.setAutoCommit(false);
        try {
            preparation.prepare();
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
            recordAppliedChecksum(connection, resource, checksum);
            if (immutable) connection.commit();
            logger.info("{} applied successfully for checksum {}.", resource, shortChecksum(checksum));
        } catch (Exception failure) {
            if (immutable) connection.rollback();
            throw failure;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private void setRuntimeRole(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT set_config('app.runtime_role', ?, true)")) {
            statement.setString(1, runtimeRole);
            statement.execute();
        }
    }

    private void requirePostgres(Connection connection) throws SQLException {
        String databaseName = connection.getMetaData().getDatabaseProductName();
        if (databaseName == null || !databaseName.toLowerCase(Locale.ROOT).contains("postgresql")) {
            throw new IllegalStateException("SQL migrations can only be run automatically against PostgreSQL.");
        }
    }

    private void ensureStateTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS public.app_migration_state (
                    id TEXT PRIMARY KEY,
                    flag_value TEXT NOT NULL,
                    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private String findLastAppliedChecksum(Connection connection, String id) throws SQLException {
        String sql = "SELECT flag_value FROM public.app_migration_state WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("flag_value") : null;
            }
        }
    }

    private void recordAppliedChecksum(Connection connection, String id, String checksum) throws SQLException {
        String sql = """
                INSERT INTO public.app_migration_state (id, flag_value, applied_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (id) DO UPDATE
                    SET flag_value = EXCLUDED.flag_value,
                        applied_at = EXCLUDED.applied_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, checksum);
            statement.executeUpdate();
        }
    }

    private void acquireMigrationLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_lock(?)")) {
            statement.setLong(1, MIGRATION_LOCK_ID);
            statement.execute();
        }
    }

    private void releaseMigrationLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            statement.setLong(1, MIGRATION_LOCK_ID);
            statement.execute();
        }
    }

    private void rollbackOpenTransaction(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            connection.rollback();
            connection.setAutoCommit(true);
        }
    }

    private String readResource(String resource) throws Exception {
        try (var input = new ClassPathResource(resource).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String shortChecksum(String checksum) {
        if (checksum == null || checksum.isBlank()) {
            return "<none>";
        }
        return checksum.length() <= 12 ? checksum : checksum.substring(0, 12);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    @FunctionalInterface
    private interface SqlPreparation {
        void prepare() throws Exception;
    }
}
