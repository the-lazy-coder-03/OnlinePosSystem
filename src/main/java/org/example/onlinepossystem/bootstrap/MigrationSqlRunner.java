package org.example.onlinepossystem.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MigrationSqlRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(MigrationSqlRunner.class);
    private static final String STATE_ID = "migration.sql";

    private final DataSource dataSource;
    private final Resource migrationScript;
    private final boolean enabled;

    public MigrationSqlRunner(
            DataSource dataSource,
            @Value("classpath:migration.sql") Resource migrationScript,
            @Value("${app.database.migration.enabled:false}") boolean enabled
    ) {
        this.dataSource = dataSource;
        this.migrationScript = migrationScript;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!enabled) {
            logger.info("migration.sql startup runner is disabled.");
            return;
        }

        String sql = readMigrationScript();
        String checksum = sha256(sql);

        try (Connection connection = dataSource.getConnection()) {
            requirePostgres(connection);
            boolean originalAutoCommit = connection.getAutoCommit();
            if (!originalAutoCommit) {
                connection.setAutoCommit(true);
            }
            try {
                ensureStateTable(connection);
                String lastAppliedChecksum = findLastAppliedChecksum(connection);
                if (checksum.equals(lastAppliedChecksum)) {
                    logger.info("migration.sql already applied for checksum {}.", shortChecksum(checksum));
                    return;
                }

                logger.warn(
                        "Running migration.sql because the checksum changed from {} to {}.",
                        shortChecksum(lastAppliedChecksum),
                        shortChecksum(checksum)
                );
                runMigrationScript(connection, sql);
                recordAppliedChecksum(connection, checksum);
                logger.info("migration.sql applied successfully for checksum {}.", shortChecksum(checksum));
            } finally {
                if (!originalAutoCommit) {
                    connection.setAutoCommit(false);
                }
            }
        }
    }

    private void requirePostgres(Connection connection) throws SQLException {
        String databaseName = connection.getMetaData().getDatabaseProductName();
        if (databaseName == null || !databaseName.toLowerCase(Locale.ROOT).contains("postgresql")) {
            throw new IllegalStateException("migration.sql can only be run automatically against PostgreSQL.");
        }
    }

    private void ensureStateTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS app_migration_state (
                    id TEXT PRIMARY KEY,
                    flag_value TEXT NOT NULL,
                    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private String findLastAppliedChecksum(Connection connection) throws SQLException {
        String sql = "SELECT flag_value FROM app_migration_state WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, STATE_ID);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("flag_value") : null;
            }
        }
    }

    private String readMigrationScript() throws Exception {
        try (InputStreamReader reader = new InputStreamReader(
                migrationScript.getInputStream(),
                StandardCharsets.UTF_8
        )) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    private void runMigrationScript(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void recordAppliedChecksum(Connection connection, String checksum) throws SQLException {
        String sql = """
                INSERT INTO app_migration_state (id, flag_value, applied_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (id) DO UPDATE
                    SET flag_value = EXCLUDED.flag_value,
                        applied_at = EXCLUDED.applied_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, STATE_ID);
            statement.setString(2, checksum);
            statement.executeUpdate();
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private String shortChecksum(String checksum) {
        if (checksum == null || checksum.isBlank()) {
            return "<none>";
        }
        return checksum.length() <= 12 ? checksum : checksum.substring(0, 12);
    }
}
