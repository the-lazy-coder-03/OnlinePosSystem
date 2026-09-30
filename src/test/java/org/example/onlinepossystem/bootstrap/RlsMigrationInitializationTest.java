package org.example.onlinepossystem.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RlsMigrationInitializationTest {

    @Test
    void migrationRunnerAppliesCatalogBeforeRlsUsingProvisionedRuntimeRole() throws IOException {
        String source = sourceFile("src/main/java/org/example/onlinepossystem/bootstrap/MigrationSqlRunner.java");

        assertThat(source)
                .contains("private static final String CATALOG_MIGRATION_ID = \"migration.sql\"")
                .contains("private static final String RLS_MIGRATION_ID = \"db/rls-v1.sql\"")
                .contains("private static final String RLS_HARDENING_MIGRATION_ID = \"db/rls-v3-hardening.sql\"")
                .contains("private static final String RLS_AUDIT_MIGRATION_ID = \"db/rls-v4-audit.sql\"")
                .contains("private static final String RLS_ADDRESS_MIGRATION_ID = \"db/rls-v5-address.sql\"")
                .contains("private static final String CATALOG_MIGRATION_RESOURCE = \"sql/migration.sql\"")
                .contains("private static final String RLS_MIGRATION_RESOURCE = \"sql/rls-v1.sql\"")
                .contains("setRuntimeRole(connection)")
                .contains("synchronizeContextSecret(connection)")
                .contains("RLS_CONTEXT_SECRET must contain at least 32 characters")
                .contains("An applied immutable migration was changed")
                .contains("recordAppliedChecksum(connection, migrationId, checksum)");

        assertThat(sourceFile("scripts/provision-rls.sh"))
                .contains("SQL files/provision-rls.sql")
                .contains("MIGRATION_DATASOURCE_USERNAME")
                .contains("SPRING_DATASOURCE_USERNAME");
        assertThat(sourceFile("SQL files/provision-rls.sql"))
                .contains("CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public");

        assertThat(source.indexOf("applyCatalogMigration(connection)"))
                .isLessThan(source.indexOf("applyRlsMigration(connection)"));
    }

    @Test
    void rlsMigrationIsRerunnableWithoutDroppingApplicationData() throws IOException {
        String sql = new ClassPathResource("sql/rls-v1.sql").getContentAsString(StandardCharsets.UTF_8);
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

    @Test
    void consolidatedSqlDirectoryIsPackagedUnderOneClasspathPrefix() {
        assertThat(new ClassPathResource("sql/migration.sql").exists()).isTrue();
        assertThat(new ClassPathResource("sql/rls-v1.sql").exists()).isTrue();
        assertThat(new ClassPathResource("sql/rls-v2-specials.sql").exists()).isTrue();
        assertThat(new ClassPathResource("sql/rls-v3-hardening.sql").exists()).isTrue();
        assertThat(new ClassPathResource("sql/rls-v4-audit.sql").exists()).isTrue();
        assertThat(new ClassPathResource("sql/rls-v5-address.sql").exists()).isTrue();
        assertThat(new ClassPathResource("sql/rls-contract-query.sql").exists()).isTrue();
        assertThat(new ClassPathResource("sql/provision-rls.sql").exists()).isTrue();
        assertThat(new ClassPathResource("sql/tessql.sql").exists()).isTrue();
    }

    @Test
    void releasedRlsMigrationsRetainTheirProductionChecksums() throws Exception {
        Map<String, String> expected = Map.of(
                "sql/rls-v1.sql", "88ad125eff51d1da04d5249e7c7f76e6700caca11cf23496203a1937fb135bc0",
                "sql/rls-v2-specials.sql", "61778ad7a0ef20027ac74920111919486fdcc418397fb085c2399e3ec6871edf",
                "sql/rls-v3-hardening.sql", "fa794d59c6a4d37452fd194e9e35175e9bc4e9edcc55392ced7f498d30419801",
                "sql/rls-v4-audit.sql", "7f9ada211a9904e40f4382fa144be4f53223a7240c57e34af0c3d62c169c0079",
                "sql/rls-v5-address.sql", "7eaefaf2b62d0af0b42d40d646302e86ecedafc638bb6bd5f31e885c7a474395"
        );

        for (Map.Entry<String, String> migration : expected.entrySet()) {
            byte[] bytes = new ClassPathResource(migration.getKey()).getContentAsByteArray();
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            assertThat(actual).as(migration.getKey()).isEqualTo(migration.getValue());
        }
    }

    @Test
    void postgresTestHarnessDoesNotOverrideTheIsolatedMigrationDatasource() throws IOException {
        String script = sourceFile("scripts/test-postgres.sh");

        assertThat(script)
                .contains("unset SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD")
                .doesNotContain("export SPRING_DATASOURCE_URL=")
                .doesNotContain("export SPRING_DATASOURCE_USERNAME=")
                .doesNotContain("export SPRING_DATASOURCE_PASSWORD=");
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
