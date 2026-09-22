package org.example.onlinepossystem.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RlsMigrationInitializationTest {

    @Test
    void migrationRunnerAppliesCatalogBeforeRlsUsingProvisionedRuntimeRole() throws IOException {
        String source = sourceFile("src/main/java/org/example/onlinepossystem/bootstrap/MigrationSqlRunner.java");

        assertThat(source)
                .contains("private static final String CATALOG_MIGRATION = \"migration.sql\"")
                .contains("private static final String RLS_MIGRATION = \"db/rls-v1.sql\"")
                .contains("setRuntimeRole(connection)")
                .contains("An applied immutable migration was changed")
                .contains("recordAppliedChecksum(connection, resource, checksum)");

        assertThat(sourceFile("scripts/provision-rls.sh"))
                .contains("sql/provision-rls.sql")
                .contains("MIGRATION_DATASOURCE_USERNAME")
                .contains("SPRING_DATASOURCE_USERNAME");

        assertThat(source.indexOf("applyCatalogMigration(connection)"))
                .isLessThan(source.indexOf("applyRlsMigration(connection)"));
    }

    @Test
    void rlsMigrationIsRerunnableWithoutDroppingApplicationData() throws IOException {
        String sql = new ClassPathResource("db/rls-v1.sql").getContentAsString(StandardCharsets.UTF_8);
        String lowered = sql.toLowerCase();

        assertThat(sql)
                .contains("CREATE SCHEMA IF NOT EXISTS app_security")
                .contains("CREATE OR REPLACE FUNCTION app_security.account_context")
                .contains("CREATE OR REPLACE FUNCTION app_security.login_credentials")
                .contains("DROP POLICY IF EXISTS orders_read ON public.customer_order")
                .contains("DROP POLICY IF EXISTS customers_read ON public.customers")
                .contains("DROP TRIGGER IF EXISTS guard_account_update ON public.customers")
                .contains("DROP TRIGGER IF EXISTS guard_order_relationship ON public.customer_order")
                .contains("current_setting('app.runtime_role')");

        assertThat(lowered)
                .doesNotContain("drop database")
                .doesNotContain("drop schema")
                .doesNotContain("drop table")
                .doesNotContain("truncate ");
    }

    private String sourceFile(String relativePath) throws IOException {
        Path workingDirectory = Path.of(System.getProperty("user.dir"));
        Path direct = workingDirectory.resolve(relativePath);
        if (Files.exists(direct)) {
            return Files.readString(direct);
        }
        return Files.readString(workingDirectory.getParent().resolve(relativePath));
    }
}
