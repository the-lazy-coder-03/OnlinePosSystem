package org.example.onlinepossystem.security.rls;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

/** Validate security before Hibernate and the HTTP server can start. */
@Configuration(proxyBeanMethods = false)
public class RlsRuntimeVerifier {
    public static final Map<String, String[]> REQUIRED_POLICIES = requiredPolicies();

    private static Map<String, String[]> requiredPolicies() {
        Map<String, String[]> policies = new LinkedHashMap<>();
        policies.put("customers", new String[]{"customers_read", "customers_insert", "customers_update", "customers_delete"});
        policies.put("customer_order", new String[]{"orders_read", "orders_insert", "orders_update", "orders_delete"});
        for (String table : new String[]{"order_menu_item", "order_menu_item_extra", "order_burger_protein",
                "order_burger_removed_component", "order_burger_extra_component", "order_pizza_item",
                "order_pizza_item_extra", "order_pizza_item_base_option"}) {
            policies.put(table, new String[]{"child_read", "child_insert", "child_update", "child_delete"});
        }
        policies.put("customer_notes", new String[]{"customer_notes_read", "customer_notes_insert"});
        policies.put("staff", new String[]{"staff_admin"});
        policies.put("password_reset_tokens", new String[]{});
        return Map.copyOf(policies);
    }

    @Bean
    public EntityManagerFactoryBuilderCustomizer verifyRlsBeforeJpa(DataSource dataSource, Environment environment,
            @Value("${app.rls.enabled:true}") boolean enabled) {
        return builder -> {
            try (Connection connection = dataSource.getConnection()) {
                if ("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())
                        && environment.matchesProfiles("postgres-test") && !enabled
                        && RlsRuntimeVerifier.class.getClassLoader().getResource("postgres-test-only.marker") != null) return;
                if (!enabled || !"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                    throw new IllegalStateException("Runtime requires PostgreSQL with RLS enabled");
                }
                verify(connection);
            } catch (SQLException exception) {
                throw new IllegalStateException("RLS runtime verification failed; run owner migrations and check grants", exception);
            }
        };
    }

    public static void verify(Connection connection) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery("""
                SELECT rolsuper OR rolbypassrls OR rolcreaterole OR rolcreatedb OR rolreplication
                    OR has_database_privilege(current_user, current_database(), 'CREATE')
                    OR has_parameter_privilege(current_user, 'session_replication_role', 'SET')
                    OR has_schema_privilege(current_user, 'public', 'CREATE')
                    OR has_schema_privilege(current_user, 'app_security', 'CREATE')
                    OR EXISTS (SELECT 1 FROM pg_roles privileged
                        WHERE (privileged.rolsuper OR privileged.rolbypassrls OR privileged.rolcreaterole
                            OR privileged.rolcreatedb OR privileged.rolreplication)
                          AND pg_has_role(current_user, privileged.oid, 'MEMBER'))
                FROM pg_roles WHERE rolname=current_user
                """)) {
            if (!result.next() || result.getBoolean(1)) throw new IllegalStateException("Runtime database role has unsafe privileges");
        }
        for (var table : REQUIRED_POLICIES.entrySet()) verifyTable(connection, table.getKey(), table.getValue());
        try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT to_regclass('public.orders') IS NOT NULL")) {
            if (result.next() && result.getBoolean(1)) {
                verifyTable(connection, "orders", new String[]{});
                verifyLegacyPolicies(connection);
            }
        }
        try (var statement = connection.createStatement(); var result = statement.executeQuery("""
                SELECT EXISTS (
                    SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
                    WHERE n.nspname='app_security' AND (pg_has_role(current_user, p.proowner, 'MEMBER')
                        OR (p.prosecdef AND (p.proconfig IS NULL OR NOT ('search_path=pg_catalog, pg_temp'=ANY(p.proconfig)))))
                )
                """)) {
            if (!result.next() || result.getBoolean(1)) throw new IllegalStateException("Unsafe RLS function ownership or search path");
        }
        verifyContract(connection);
        verifyFunctionGrants(connection);
    }

    private static void verifyContract(Connection connection) throws SQLException {
        String originalPath;
        try (var statement = connection.createStatement(); var result = statement.executeQuery("SHOW search_path")) {
            result.next();
            originalPath = result.getString(1);
        }
        try {
            setSearchPath(connection, "pg_catalog");
            var mapper = new ObjectMapper();
            JsonNode expected = mapper.readTree(resource("config/rls-contract.json"));
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery(resource("db/rls-contract-query.sql"))) {
                if (!result.next()) throw new IllegalStateException("Missing RLS security contract");
                JsonNode actual = mapper.readTree(result.getString(1));
                for (String section : new String[]{"policies", "triggers", "functions"}) {
                    if (!expected.path(section).equals(actual.path(section))) {
                        throw new IllegalStateException("RLS " + section + " differ from the reviewed security contract");
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read the RLS security contract", exception);
        } finally {
            setSearchPath(connection, originalPath);
        }
    }

    private static void setSearchPath(Connection connection, String path) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT pg_catalog.set_config('search_path', ?, false)")) {
            statement.setString(1, path);
            statement.execute();
        }
    }

    private static String resource(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }

    private static void verifyLegacyPolicies(Connection connection) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery("""
                SELECT EXISTS (SELECT 1 FROM pg_policy p JOIN pg_class c ON c.oid=p.polrelid
                    WHERE c.oid='public.orders'::regclass AND NOT (
                        p.polname='owner_maintenance' AND p.polroles=ARRAY[c.relowner]
                        AND p.polcmd='*' AND p.polpermissive
                        AND pg_get_expr(p.polqual, p.polrelid)='true'
                        AND pg_get_expr(p.polwithcheck, p.polrelid)='true'))
                """)) {
            result.next();
            if (result.getBoolean(1)) throw new IllegalStateException("Unexpected policy on legacy orders");
        }
    }

    private static void verifyFunctionGrants(Connection connection) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery("""
                SELECT EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
                    WHERE n.nspname='app_security' AND (
                        EXISTS (SELECT 1 FROM aclexplode(coalesce(p.proacl, acldefault('f', p.proowner))) a
                            WHERE a.grantee=0)
                        OR has_function_privilege(current_user, p.oid, 'EXECUTE WITH GRANT OPTION')
                        OR has_function_privilege(current_user, p.oid, 'EXECUTE')
                            <> (p.proname NOT IN ('guard_account_update', 'guard_order_relationship'))))
                """)) {
            result.next();
            if (result.getBoolean(1)) throw new IllegalStateException("Unsafe RLS function grants");
        }
    }

    private static void verifyTable(Connection connection, String table, String[] policies) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT c.relrowsecurity AND c.relforcerowsecurity
                    AND NOT pg_has_role(current_user, c.relowner, 'MEMBER')
                    AND NOT has_table_privilege(current_user, c.oid, 'TRUNCATE')
                FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                WHERE n.nspname='public' AND c.relname=? AND c.relkind='r'
                """)) {
            statement.setString(1, table);
            try (var result = statement.executeQuery()) {
                if (!result.next() || !result.getBoolean(1)) throw new IllegalStateException("Missing or unsafe RLS on " + table);
            }
        }
        verifyTableGrants(connection, table);
        for (String policy : policies) {
            try (var statement = connection.prepareStatement("SELECT 1 FROM pg_policies WHERE schemaname='public' AND tablename=? AND policyname=?")) {
                statement.setString(1, table);
                statement.setString(2, policy);
                try (var result = statement.executeQuery()) {
                    if (!result.next()) throw new IllegalStateException("Missing required RLS policy on " + table);
                }
            }
        }
    }

    private static void verifyTableGrants(Connection connection, String table) throws SQLException {
        var allowed = switch (table) {
            case "password_reset_tokens", "orders" -> java.util.Set.of("SELECT");
            case "customer_notes" -> java.util.Set.of("SELECT", "INSERT");
            default -> java.util.Set.of("SELECT", "INSERT", "UPDATE", "DELETE");
        };
        try (var statement = connection.prepareStatement("""
                SELECT has_table_privilege(current_user, c.oid, ?),
                    has_table_privilege(current_user, c.oid, ?),
                    EXISTS (SELECT 1 FROM aclexplode(coalesce(c.relacl, acldefault('r', c.relowner))) a
                        WHERE a.grantee=0)
                FROM pg_class c WHERE c.oid=to_regclass(?)
                """)) {
            for (String privilege : new String[]{"SELECT", "INSERT", "UPDATE", "DELETE", "TRUNCATE", "REFERENCES", "TRIGGER"}) {
                statement.setString(1, privilege);
                statement.setString(2, privilege + " WITH GRANT OPTION");
                statement.setString(3, "public." + table);
                try (var result = statement.executeQuery()) {
                    if (!result.next() || result.getBoolean(1) != allowed.contains(privilege)
                            || result.getBoolean(2) || result.getBoolean(3)) {
                        throw new IllegalStateException("Unsafe " + privilege + " grants on " + table);
                    }
                }
            }
        }
    }
}
