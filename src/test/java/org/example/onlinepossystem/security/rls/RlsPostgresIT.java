package org.example.onlinepossystem.security.rls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.onlinepossystem.MigrationConfiguration;
import org.example.onlinepossystem.OnlinePosSystemApplication;
import org.example.onlinepossystem.bootstrap.MigrationSqlRunner;
import org.example.onlinepossystem.customer.persistence.AccountBootstrapStore;
import org.example.onlinepossystem.security.api.AccountPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RlsPostgresIT {
    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private final String database = "rls_test_" + suffix;
    private final String owner = "rls_owner_" + suffix;
    private final String runtime = "rls_runtime_" + suffix;
    private final String password = "isolated-test-only";
    private String adminUrl;
    private String adminUser;
    private String adminPassword;
    private String jdbcUrl;
    private ConfigurableApplicationContext application;
    private HikariDataSource pool;
    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;
    private MockMvc mvc;
    private UserDetailsService accounts;
    private long customerA;
    private long customerB;
    private long branchOne;
    private long branchTwo;
    private long namedSuperAdmin;
    private long driver;
    private long orderA;
    private long orderB;
    private boolean rolesCreated;
    private boolean databaseCreated;

    @BeforeAll
    void startIsolatedDatabaseAndApplication() throws Exception {
        adminUrl = required("RLS_TEST_ADMIN_URL");
        adminUser = required("RLS_TEST_ADMIN_USERNAME");
        adminPassword = System.getenv().getOrDefault("RLS_TEST_ADMIN_PASSWORD", "");
        int slash = adminUrl.lastIndexOf('/');
        jdbcUrl = adminUrl.substring(0, slash + 1) + database;
        try (Connection connection = adminConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE ROLE " + owner + " LOGIN NOSUPERUSER NOBYPASSRLS PASSWORD '" + password + "'");
            statement.execute("CREATE ROLE " + runtime + " LOGIN NOSUPERUSER NOBYPASSRLS NOINHERIT PASSWORD '" + password + "'");
            rolesCreated = true;
            statement.execute("CREATE DATABASE " + database + " OWNER " + owner);
            databaseCreated = true;
        }
        migrate();
        // Reapplying uses recorded checksums and does not duplicate policies or data.
        migrate();
        seed();
        SpringApplication app = new SpringApplication(OnlinePosSystemApplication.class);
        application = app.run(runtimeArguments());
        pool = application.getBean(HikariDataSource.class);
        jdbc = application.getBean(JdbcTemplate.class);
        var activeManager = application.getBean(PlatformTransactionManager.class);
        assertThat(activeManager).isInstanceOf(org.springframework.orm.jpa.JpaTransactionManager.class);
        assertThat(((org.springframework.orm.jpa.JpaTransactionManager) activeManager).getJpaDialect()).isInstanceOf(RlsJpaDialect.class);
        transaction = new TransactionTemplate(activeManager);
        mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) application).apply(springSecurity()).build();
        accounts = application.getBean(UserDetailsService.class);
    }

    private void migrate() {
        SpringApplication app = new SpringApplication(MigrationConfiguration.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        try (var context = app.run(
                "--spring.profiles.active=migrate", "--spring.config.import=",
                "--MIGRATION_DATASOURCE_URL=" + jdbcUrl, "--MIGRATION_DATASOURCE_USERNAME=" + owner,
                "--MIGRATION_DATASOURCE_PASSWORD=" + password, "--app.rls.runtime-role=" + runtime,
                "--app.database.migration.enabled=true", "--spring.jpa.show-sql=false",
                "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect")) {
            assertThat(context.getBean(MigrationSqlRunner.class)).isNotNull();
        }
    }

    private String[] runtimeArguments() {
        return new String[]{"--spring.profiles.active=rls-test", "--spring.config.import=",
                "--spring.datasource.url=" + jdbcUrl, "--spring.datasource.username=" + runtime,
                "--spring.datasource.password=" + password, "--spring.datasource.driver-class-name=org.postgresql.Driver",
                "--spring.datasource.hikari.maximum-pool-size=1", "--spring.datasource.hikari.minimum-idle=1",
                "--spring.datasource.hikari.connection-timeout=5000", "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.jpa.open-in-view=false", "--spring.jpa.show-sql=false",
                "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
                "--app.rls.enabled=true", "--app.database.migration.enabled=false", "--server.port=0",
                "--server.address=127.0.0.1", "--ADMIN_USERNAME=environment-admin", "--ADMIN_PASSWORD=test-admin-password",
                "--jwt.secret=integration-test-only-secret-at-least-32-bytes", "--resend.api-key=",
                "--SESSION_COOKIE_SECURE=false", "--logging.level.org.springframework.security=WARN"};
    }

    private void seed() throws SQLException {
        try (Connection c = ownerConnection()) {
            customerA = insert(c, "INSERT INTO customers(email,password,access_level,role) VALUES ('a@example.com','{noop}customer-pass',0,'USER') RETURNING id");
            customerB = insert(c, "INSERT INTO customers(email,password,access_level,role) VALUES ('b@example.com','{noop}customer-pass',0,'USER') RETURNING id");
            branchOne = insert(c, "INSERT INTO customers(email,password,access_level,role) VALUES ('one@example.com','{noop}admin-pass',1,'ADMIN') RETURNING id");
            branchTwo = insert(c, "INSERT INTO customers(email,password,access_level,role) VALUES ('two@example.com','{noop}admin-pass',2,'ADMIN') RETURNING id");
            namedSuperAdmin = insert(c, "INSERT INTO customers(email,password,access_level,role) VALUES ('super@example.com','{noop}admin-pass',3,'SUPER_ADMIN') RETURNING id");
            driver = insert(c, "INSERT INTO customers(email,password,access_level,role) VALUES ('driver@example.com','{noop}driver-pass',4,'DRIVER') RETURNING id");
            orderA = insert(c, "INSERT INTO customer_order(branch_id,customer_id,status,created_at,order_type) VALUES (1,"+customerA+",'Pending',CURRENT_TIMESTAMP,'pickup') RETURNING order_id");
            orderB = insert(c, "INSERT INTO customer_order(branch_id,customer_id,status,created_at,order_type) VALUES (2,"+customerB+",'Pending',CURRENT_TIMESTAMP,'pickup') RETURNING order_id");
            try (var s = c.createStatement()) {
                s.execute("UPDATE customers SET phone1='0123456789' WHERE id=" + customerA);
                // The legacy table is intentionally not part of the active entity model.
                s.execute("CREATE TABLE orders(id bigint PRIMARY KEY, customer_name text)");
                s.execute("INSERT INTO orders VALUES(1, 'Legacy private data')");
                s.execute("ALTER TABLE orders ENABLE ROW LEVEL SECURITY; ALTER TABLE orders FORCE ROW LEVEL SECURITY");
                s.execute("GRANT SELECT ON orders TO " + runtime);
            }
        }
    }

    private long insert(Connection c, String sql) throws SQLException {
        try (var s = c.createStatement(); var r = s.executeQuery(sql)) { r.next(); return r.getLong(1); }
    }

    @AfterEach void clearIdentity() { SecurityContextHolder.clearContext(); }

    @Test @Order(0)
    void verifierRejectsPolicyTriggerFunctionAndGrantDrift() throws Exception {
        var changes = new ArrayList<>(List.of(
                "ALTER POLICY orders_read ON customer_order USING (true)",
                "ALTER POLICY orders_insert ON customer_order WITH CHECK (true)",
                "ALTER POLICY orders_read ON customer_order TO " + owner,
                "ALTER POLICY owner_maintenance ON customers TO PUBLIC",
                "DROP POLICY orders_read ON customer_order; CREATE POLICY orders_read ON customer_order USING (true)",
                "ALTER TABLE customers DISABLE TRIGGER guard_account_update",
                "ALTER TABLE order_menu_item DISABLE TRIGGER guard_order_relationship",
                "DROP TRIGGER guard_order_relationship ON customer_order",
                "ALTER FUNCTION app_security.actor_role() RENAME TO altered_actor_role",
                "CREATE OR REPLACE FUNCTION app_security.guard_account_update() RETURNS trigger LANGUAGE plpgsql AS 'BEGIN RETURN NEW; END'",
                "GRANT EXECUTE ON FUNCTION app_security.login_credentials(text) TO PUBLIC",
                "GRANT UPDATE ON password_reset_tokens TO " + runtime,
                "GRANT DELETE ON customer_notes TO " + runtime,
                "GRANT SELECT ON customers TO PUBLIC",
                "GRANT SELECT ON customers TO " + runtime + " WITH GRANT OPTION",
                "GRANT TRUNCATE ON customer_order TO " + runtime,
                "GRANT CREATE ON DATABASE " + database + " TO " + runtime,
                "GRANT " + owner + " TO " + runtime,
                "GRANT SET ON PARAMETER session_replication_role TO " + runtime,
                "CREATE POLICY leaked_legacy ON orders FOR SELECT USING (true)"
        ));
        for (String table : RlsRuntimeVerifier.REQUIRED_POLICIES.keySet()) {
            changes.add("CREATE POLICY leaked_rows ON " + table + " FOR SELECT USING (true)");
            changes.add("ALTER TABLE " + table + " NO FORCE ROW LEVEL SECURITY");
        }
        for (String change : changes) {
            // Transactional DDL and SET LOCAL ROLE let the verifier observe each unsafe
            // configuration as runtime, without exposing it to another test connection.
            try (Connection c = DriverManager.getConnection(jdbcUrl, adminUser, adminPassword)) {
                c.setAutoCommit(false);
                try (var s = c.createStatement()) {
                    s.execute(change);
                    s.execute("SET LOCAL ROLE " + runtime);
                    assertThatThrownBy(() -> RlsRuntimeVerifier.verify(c)).as(change)
                            .isInstanceOf(IllegalStateException.class);
                } finally { c.rollback(); }
            }
        }
        try (Connection c = pool.getConnection()) { RlsRuntimeVerifier.verify(c); }
    }

    @Test @Order(0)
    void disabledProtectionTriggerPreventsApplicationStartup() throws Exception {
        try (Connection c = ownerConnection(); var s = c.createStatement()) {
            s.execute("ALTER TABLE customers DISABLE TRIGGER guard_account_update");
            try {
                assertThatThrownBy(() -> {
                    try (var ignored = new SpringApplication(OnlinePosSystemApplication.class).run(runtimeArguments())) { }
                }).hasStackTraceContaining("RLS triggers differ from the reviewed security contract");
            } finally { s.execute("ALTER TABLE customers ENABLE TRIGGER guard_account_update"); }
        }
    }

    @Test @Order(0)
    void bearerIdentityOverridesSessionWithoutChangingIt() throws Exception {
        var session = (MockHttpSession) mvc.perform(post("/login").with(csrf())
                        .param("username", "environment-admin").param("password", "test-admin-password"))
                .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
        String token = application.getBean(org.example.onlinepossystem.security.api.TokenService.class)
                .generateToken(accounts.loadUserByUsername("a@example.com"));
        mvc.perform(get("/api/admin/orders").session(session).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/orders").session(session)).andExpect(status().isOk());
    }

    @Test @Order(1)
    void runtimeIsRestrictedAndEveryProtectedTableHasForcedRls() throws Exception {
        try (Connection c = pool.getConnection()) { RlsRuntimeVerifier.verify(c); }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relrowsecurity AND c.relforcerowsecurity", Integer.class)).isEqualTo(RlsRuntimeVerifier.REQUIRED_POLICIES.size() + 1);
        int childPolicyCount = RlsRuntimeVerifier.REQUIRED_POLICIES.values().stream()
                .flatMap(Arrays::stream).mapToInt(policy -> policy.startsWith("child_") ? 1 : 0).sum();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_policies WHERE schemaname='public' AND policyname LIKE 'child_%'", Integer.class))
                .isEqualTo(childPolicyCount);
        try (Connection c = ownerConnection()) {
            assertThatThrownBy(() -> RlsRuntimeVerifier.verify(c)).isInstanceOf(IllegalStateException.class);
        }
        assertThatThrownBy(() -> jdbc.execute("TRUNCATE customer_order CASCADE")).hasRootCauseInstanceOf(SQLException.class);
        assertThatThrownBy(() -> jdbc.execute("SET ROLE " + owner)).hasRootCauseInstanceOf(SQLException.class);
    }

    @Test @Order(1)
    void seedsCompleteKenridgeAndUitzichtSpecialMatricesAsReadOnlyPublicData() {
        Map<String, BigDecimal> prices = jdbc.query(
                "SELECT code, bundle_price FROM special ORDER BY code",
                result -> {
                    Map<String, BigDecimal> values = new LinkedHashMap<>();
                    while (result.next()) values.put(result.getString(1), result.getBigDecimal(2));
                    return values;
                });

        assertThat(prices).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("KEN-MON-2-LARGE-FAVOURITES", new BigDecimal("234.00")),
                Map.entry("KEN-MON-STEAK-BURGERS", new BigDecimal("330.00")),
                Map.entry("KEN-TUE-FAVOURITE-SUPREME", new BigDecimal("246.00")),
                Map.entry("KEN-TUE-CHEESE-BURGERS", new BigDecimal("170.00")),
                Map.entry("KEN-WED-2-LARGE-SUPREMES", new BigDecimal("260.00")),
                Map.entry("KEN-WED-TOASTIES", new BigDecimal("135.00")),
                Map.entry("KEN-THU-RIBS", new BigDecimal("290.00")),
                Map.entry("KEN-SUN-PASTA", new BigDecimal("169.00")),
                Map.entry("KEN-SUN-RIBS-PIZZAS", new BigDecimal("365.00")),
                Map.entry("UIT-MON-2-LARGE-FAVOURITES", new BigDecimal("225.00")),
                Map.entry("UIT-MON-STEAK-BURGERS", new BigDecimal("295.00")),
                Map.entry("UIT-TUE-FAVOURITE-SUPREME", new BigDecimal("236.00")),
                Map.entry("UIT-TUE-CHEESE-BURGERS", new BigDecimal("160.00")),
                Map.entry("UIT-WED-2-LARGE-SUPREMES", new BigDecimal("247.00")),
                Map.entry("UIT-WED-TOASTIES", new BigDecimal("120.00")),
                Map.entry("UIT-THU-RIBS", new BigDecimal("276.00")),
                Map.entry("UIT-SUN-PASTA", new BigDecimal("159.00")),
                Map.entry("UIT-SUN-RIBS-PIZZAS", new BigDecimal("360.00"))
        ));
        assertThat(jdbc.queryForList("SELECT DISTINCT day_of_week FROM special_day ORDER BY day_of_week", Integer.class))
                .containsExactly(1, 2, 3, 4, 7);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM special WHERE branch_id=1", Integer.class)).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM special WHERE branch_id=2", Integer.class)).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM special_component", Integer.class)).isEqualTo(38);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM special_component_menu_item", Integer.class)).isEqualTo(40);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM special_component_pizza", Integer.class)).isEqualTo(68);
        assertThat(jdbc.queryForObject("SELECT price FROM special_addon a JOIN special s ON s.special_id=a.special_id WHERE s.code='KEN-THU-RIBS'", BigDecimal.class))
                .isEqualByComparingTo("100.00");
        assertThat(jdbc.queryForObject("SELECT price FROM special_addon a JOIN special s ON s.special_id=a.special_id WHERE s.code='UIT-THU-RIBS'", BigDecimal.class))
                .isEqualByComparingTo("95.00");
        assertThat(jdbc.update("UPDATE special SET active=false WHERE code='UIT-MON-STEAK-BURGERS'"))
                .isZero();
    }

    @Test @Order(2)
    void customersAndBothBranchesAreIsolatedWithoutJavaFilters() {
        as("a@example.com");
        assertThat(ids("customer_order", "order_id")).containsExactly(orderA);
        assertThat(ids("customers", "id")).containsExactly(customerA);
        as("b@example.com");
        assertThat(ids("customer_order", "order_id")).containsExactly(orderB);
        assertThat(ids("customers", "id")).containsExactly(customerB);
        as("one@example.com");
        assertThat(ids("customer_order", "order_id")).containsExactly(orderA);
        assertThat(ids("customers", "id")).containsExactlyInAnyOrder(branchOne, customerA);
        as("two@example.com");
        assertThat(ids("customer_order", "order_id")).containsExactly(orderB);
        assertThat(ids("customers", "id")).containsExactlyInAnyOrder(branchTwo, customerB);
        as("environment-admin");
        assertThat(ids("customer_order", "order_id")).contains(orderA, orderB);
        as("driver@example.com");
        assertThat(ids("customer_order", "order_id")).isEmpty();
        assertThat(ids("customers", "id")).containsExactly(driver);
    }

    @Test @Order(3)
    void mutationsHavePositiveControlsAndRejectWrongOwnership() {
        as("a@example.com");
        long own = transaction.execute(s -> jdbc.queryForObject("INSERT INTO customer_order(branch_id,customer_id,created_at,order_type,status) VALUES(2,?,CURRENT_TIMESTAMP,'pickup','Pending') RETURNING order_id", Long.class, customerA));
        assertThat(own).isPositive(); // Customers may choose any public branch for their own order.
        denied(() -> transaction.<Integer>execute(s -> jdbc.update("INSERT INTO customer_order(branch_id,customer_id,created_at,order_type,status) VALUES(1,?,CURRENT_TIMESTAMP,'pickup','Pending')", customerB)));
        denied(() -> transaction.<Integer>execute(s -> jdbc.update("UPDATE customers SET access_level=3,role='SUPER_ADMIN' WHERE id=?", customerA)));
        assertThat(update("UPDATE customers SET first_name='A' WHERE id=?", customerA)).isEqualTo(1);
        assertThat(update("UPDATE customers SET first_name='Hacked' WHERE id=?", customerB)).isZero();
        assertThat(update("UPDATE customer_order SET status='Completed' WHERE order_id=?", orderB)).isZero();
        assertThat(update("DELETE FROM customer_order WHERE order_id=?", orderB)).isZero();
        as("one@example.com");
        assertThat(update("UPDATE customer_order SET status='Preparing' WHERE order_id=?", orderA)).isEqualTo(1);
        assertThat(update("UPDATE customer_order SET status='Completed' WHERE order_id=?", orderB)).isZero();
        denied(() -> transaction.<Integer>execute(s -> jdbc.update("INSERT INTO customer_order(branch_id,customer_id,created_at,order_type,status) VALUES(2,?,CURRENT_TIMESTAMP,'pickup','Pending')", customerA)));
        denied(() -> transaction.<Integer>execute(s -> jdbc.update("UPDATE customer_order SET branch_id=2 WHERE order_id=?", orderA)));
        denied(() -> transaction.<Integer>execute(s -> jdbc.update("UPDATE customer_order SET customer_id=? WHERE order_id=?", customerB, orderA)));
        assertThat(update("DELETE FROM customer_order WHERE order_id=?", orderB)).isZero();
        as("environment-admin");
        assertThat(update("DELETE FROM customer_order WHERE order_id=?", own)).isEqualTo(1);
    }

    @Test @Order(4)
    void missingEmptyMalformedAndUnknownContextsExposeNothing() {
        Set<String> publicSpecialTables = Set.of("special", "special_day", "special_component",
                "special_component_menu_item", "special_component_pizza", "special_addon");
        for (String table : RlsRuntimeVerifier.REQUIRED_POLICIES.keySet()) {
            Integer count = jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
            if (publicSpecialTables.contains(table)) assertThat(count).as(table).isPositive();
            else assertThat(count).as(table).isZero();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isZero();
        transaction.executeWithoutResult(s -> {
            jdbc.execute("SELECT set_config('app.customer_id', '', true), set_config('app.role','ADMIN',true)");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_order", Integer.class)).isZero();
            jdbc.execute("SELECT set_config('app.customer_id', 'invalid', true), set_config('app.role','UNKNOWN',true)");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM customers", Integer.class)).isZero();
        });
        denied(() -> transaction.<Integer>execute(s -> jdbc.update("INSERT INTO customer_order(branch_id,customer_id,created_at,order_type,status) VALUES(1,?,CURRENT_TIMESTAMP,'pickup','Pending')", customerA)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM menu_item", Integer.class)).isPositive();
    }

    @Test @Order(5)
    void singleConnectionNeverLeaksContextAfterCommitRollbackOrFailedInitialization() {
        assertThat(pool.getMaximumPoolSize()).isEqualTo(1);
        int backend = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
        for (String name : List.of("a@example.com", "b@example.com", "one@example.com", "environment-admin")) {
            as(name);
            transaction.executeWithoutResult(s -> {
                assertThat(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class)).isEqualTo(backend);
                assertThat(jdbc.queryForObject("SELECT current_setting('app.role', true)", String.class)).isNotBlank();
            });
            transaction.executeWithoutResult(s -> s.setRollbackOnly());
            SecurityContextHolder.clearContext();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_order", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT nullif(current_setting('app.customer_id',true),'')", String.class)).isNull();
        }
        RlsContextInitializer failing = new RlsContextInitializerForTest();
        var manager = new org.springframework.orm.jpa.JpaTransactionManager(application.getBean(EntityManagerFactory.class));
        manager.setJpaDialect(new RlsJpaDialect(failing));
        assertThatThrownBy(() -> new TransactionTemplate(manager).execute(s -> null)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_order", Integer.class)).isZero();
        as("b@example.com");
        assertThat(ids("customer_order", "order_id")).containsExactly(orderB);
    }

    @Test @Order(6)
    void sameJpaConnectionJoinedAndRequiresNewTransactionsUseCorrectContext() {
        // REQUIRES_NEW needs a second connection while its parent holds the first.
        pool.setMaximumPoolSize(2);
        try {
            as("a@example.com");
            transaction.executeWithoutResult(status -> {
                EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(application.getBean(EntityManagerFactory.class));
                assertThat(((Number) em.createNativeQuery("SELECT pg_backend_pid()").getSingleResult()).intValue())
                        .isEqualTo(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                assertThat(ids("customer_order", "order_id")).containsExactly(orderA);
                var previous = SecurityContextHolder.getContext().getAuthentication();
                var b = new AccountPrincipal("b@example.com", "erased", customerB, 0, false);
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(b, null, b.getAuthorities()));
                TransactionTemplate inner = new TransactionTemplate(application.getBean(PlatformTransactionManager.class));
                inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                inner.executeWithoutResult(s -> assertThat(jdbc.queryForList("SELECT order_id FROM customer_order", Long.class)).containsExactly(orderB));
                SecurityContextHolder.getContext().setAuthentication(previous);
                assertThat(jdbc.queryForList("SELECT order_id FROM customer_order", Long.class)).containsExactly(orderA);
            });
        } finally { pool.setMaximumPoolSize(1); }
    }

    @Test @Order(7)
    void loginRegistrationRecoveryAndPublicRoutesWork() throws Exception {
        mvc.perform(get("/api/orders/menu").param("branch", "Kenridge")).andExpect(status().isOk());
        mvc.perform(get("/api/orders").param("branch", "Kenridge")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/staff/login").contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"1234\"}"))
                .andExpect(status().isGone());
        mvc.perform(post("/login").with(csrf()).param("username", "0123456789").param("password", "customer-pass"))
                .andExpect(status().is3xxRedirection()).andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a@example.com\",\"password\":\"customer-pass\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        AccountBootstrapStore store = application.getBean(AccountBootstrapStore.class);
        var c = new org.example.onlinepossystem.customer.entity.Customer();
        c.setEmail("new@example.com"); c.setPassword("{noop}new-pass"); c.setRole("SUPER_ADMIN"); c.setAccessLevel(3);
        var created = store.register(c);
        assertThat(store.findCredentials("new@example.com").orElseThrow().accessLevel()).isZero();
        String hash = "a".repeat(64);
        assertThat(store.createReset("new@example.com", hash, LocalDateTime.now().plusMinutes(30))).contains("new@example.com");
        assertThat(store.consumeReset(hash, "{noop}changed")).isTrue();
        assertThat(store.consumeReset(hash, "{noop}reuse")).isFalse();
        assertThat(store.findCredentials("new@example.com").orElseThrow().password()).isEqualTo("{noop}changed");
        String expiredHash = "c".repeat(64);
        assertThat(store.createReset("new@example.com", expiredHash, LocalDateTime.now().plusMinutes(30))).contains("new@example.com");
        try (Connection ownerDb = ownerConnection(); var statement = ownerDb.createStatement()) {
            statement.executeUpdate("UPDATE password_reset_tokens SET expires_at=LOCALTIMESTAMP - interval '1 minute' WHERE token_hash='" + expiredHash + "'");
        }
        assertThat(store.consumeReset(expiredHash, "{noop}expired")).isFalse();
        assertThat(store.findCredentials("new@example.com").orElseThrow().password()).isEqualTo("{noop}changed");
        assertThat(created.getId()).isPositive();
    }

    @Test @Order(8)
    void accessChangesApplyToAlreadyAuthenticatedPrincipal() throws Exception {
        as("one@example.com");
        try (Connection c = ownerConnection(); var s = c.createStatement()) {
            s.execute("UPDATE customers SET access_level=2 WHERE id=" + branchOne);
        }
        try { assertThat(ids("customer_order", "order_id")).containsExactly(orderB); }
        finally {
            try (Connection c = ownerConnection(); var s = c.createStatement()) {
                s.execute("UPDATE customers SET access_level=1 WHERE id=" + branchOne);
            }
        }
    }

    @Test @Order(9)
    void allOrderDescendantsFollowTheirParentsAndCannotBeReassigned() throws Exception {
        long[] menus = new long[2];
        long[] pizzas = new long[2];
        try (Connection c = ownerConnection()) {
            long[] orders = {orderA, orderB};
            for (int i=0; i<orders.length; i++) {
                menus[i] = insert(c, "INSERT INTO order_menu_item(order_id,menu_item_id,qty,unit_price_at_time) SELECT " + orders[i]
                        + ",burger_id,1,80 FROM burger_recipe_assignment ORDER BY burger_id LIMIT 1 RETURNING order_menu_item_id");
                pizzas[i] = insert(c, "INSERT INTO order_pizza_item(order_id,pizza_id,pizza_size_id,qty,base_price_at_time) SELECT " + orders[i]
                        + ",pizza_id,pizza_size_id,1,90 FROM branch_pizza_price ORDER BY pizza_id,pizza_size_id LIMIT 1 RETURNING order_pizza_item_id");
                try (var st = c.createStatement()) {
                    st.execute("INSERT INTO order_menu_item_extra(order_menu_item_id,name,qty,unit_price_at_time) VALUES("+menus[i]+",'Extra',1,1)");
                    st.execute("INSERT INTO order_burger_protein(order_menu_item_id,component_id,protein_qty_per_burger,unit_price_at_time) SELECT "
                            +menus[i]+",bc.component_id,a.protein_quantity_required,0 FROM burger_component bc CROSS JOIN burger_recipe_assignment a "
                            +"JOIN order_menu_item omi ON omi.menu_item_id=a.burger_id WHERE omi.order_menu_item_id="+menus[i]
                            +" AND bc.component_type='protein' AND bc.active ORDER BY bc.component_id LIMIT 1");
                    st.execute("INSERT INTO order_burger_removed_component(order_menu_item_id,component_id) SELECT "
                            +menus[i]+",rc.component_id FROM order_menu_item omi JOIN burger_recipe_assignment a ON a.burger_id=omi.menu_item_id "
                            +"JOIN burger_recipe_component rc ON rc.recipe_id=a.recipe_id WHERE omi.order_menu_item_id="+menus[i]
                            +" AND rc.is_removable ORDER BY rc.component_id LIMIT 1");
                    st.execute("INSERT INTO order_burger_extra_component(order_menu_item_id,component_id,qty,unit_price_at_time) SELECT "
                            +menus[i]+",component_id,1,1 FROM burger_component WHERE component_type<>'protein' AND active ORDER BY component_id LIMIT 1");
                    st.execute("INSERT INTO order_pizza_item_extra(order_pizza_item_id,ingredient_id,qty,unit_price_at_time) SELECT "
                            +pizzas[i]+",ingredient_id,1,1 FROM ingredient ORDER BY ingredient_id LIMIT 1");
                    st.execute("INSERT INTO order_pizza_item_base_option(order_pizza_item_id,pizza_base_option_id,unit_price_at_time) SELECT "
                            +pizzas[i]+",pizza_base_option_id,1 FROM pizza_base_option ORDER BY pizza_base_option_id LIMIT 1");
                }
            }
        }
        Map<String,String> parentColumns = Map.of(
                "order_menu_item", "order_id", "order_pizza_item", "order_id",
                "order_menu_item_extra", "order_menu_item_id", "order_burger_protein", "order_menu_item_id",
                "order_burger_removed_component", "order_menu_item_id", "order_burger_extra_component", "order_menu_item_id",
                "order_pizza_item_extra", "order_pizza_item_id", "order_pizza_item_base_option", "order_pizza_item_id");
        for (var entry : parentColumns.entrySet()) {
            String table = entry.getKey(); String key = entry.getValue();
            long a = key.equals("order_id") ? orderA : key.equals("order_menu_item_id") ? menus[0] : pizzas[0];
            long b = key.equals("order_id") ? orderB : key.equals("order_menu_item_id") ? menus[1] : pizzas[1];
            as("a@example.com");
            assertThat(transaction.<List<Long>>execute(t -> jdbc.queryForList("SELECT "+key+" FROM "+table, Long.class))).as(table).containsExactly(a);
            assertThat(transaction.<Integer>execute(t -> jdbc.update("DELETE FROM "+table+" WHERE "+key+"=?", b))).isZero();
            as("b@example.com");
            assertThat(transaction.<List<Long>>execute(t -> jdbc.queryForList("SELECT "+key+" FROM "+table, Long.class))).containsExactly(b);
            as("one@example.com");
            assertThat(transaction.<List<Long>>execute(t -> jdbc.queryForList("SELECT "+key+" FROM "+table, Long.class))).containsExactly(a);
            as("two@example.com");
            assertThat(transaction.<List<Long>>execute(t -> jdbc.queryForList("SELECT "+key+" FROM "+table, Long.class))).containsExactly(b);
            as("environment-admin");
            assertThat(transaction.<List<Long>>execute(t -> jdbc.queryForList("SELECT "+key+" FROM "+table, Long.class))).containsExactlyInAnyOrder(a,b);
            denied(() -> transaction.<Integer>execute(t -> jdbc.update("UPDATE "+table+" SET "+key+"=? WHERE "+key+"=?", b,a)));
        }
        as("a@example.com");
        denied(() -> transaction.<Integer>execute(t -> jdbc.update("INSERT INTO order_menu_item(order_id,menu_item_id,qty,unit_price_at_time) VALUES(?,201,1,80)", orderB)));
        denied(() -> transaction.<Integer>execute(t -> jdbc.update("INSERT INTO order_menu_item_extra(order_menu_item_id,name,qty,unit_price_at_time) VALUES(?,'Forbidden',1,1)", menus[1])));
        assertThat(transaction.<Integer>execute(t -> jdbc.update("INSERT INTO order_menu_item_extra(order_menu_item_id,name,qty,unit_price_at_time) VALUES(?,'Allowed',1,1)", menus[0]))).isEqualTo(1);
    }

    @Test @Order(10)
    void parallelRequestsAndCompetingResetConsumptionRemainIsolated() throws Exception {
        var aPrincipal = accounts.loadUserByUsername("a@example.com");
        var bPrincipal = accounts.loadUserByUsername("b@example.com");
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            List<Callable<List<Long>>> work = new ArrayList<>();
            for (int i=0; i<12; i++) {
                final var principal = i%2 == 0 ? aPrincipal : bPrincipal;
                work.add(() -> {
                    try {
                        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
                        return ids("customer_order", "order_id");
                    } finally { SecurityContextHolder.clearContext(); }
                });
            }
            var results = workers.invokeAll(work);
            for (int i=0; i<results.size(); i++) assertThat(results.get(i).get()).containsExactly(i%2==0 ? orderA : orderB);
            SecurityContextHolder.clearContext();
            var store = application.getBean(AccountBootstrapStore.class);
            String hash = "b".repeat(64);
            assertThat(store.createReset("new@example.com", hash, LocalDateTime.now().plusMinutes(30))).contains("new@example.com");
            try (Connection c = ownerConnection(); var s = c.createStatement(); var rs = s.executeQuery("SELECT count(*) FROM password_reset_tokens WHERE token_hash='" + hash + "' AND NOT used")) {
                rs.next(); assertThat(rs.getInt(1)).isEqualTo(1);
            }
            // Separate physical connections exercise the database lock and token recheck.
            pool.setMaximumPoolSize(2);
            List<Callable<Boolean>> resets = List.of(
                    () -> store.consumeReset(hash,"{noop}winner-one"), () -> store.consumeReset(hash,"{noop}winner-two"));
            var resetResults = workers.invokeAll(resets);
            assertThat(List.of(resetResults.get(0).get(), resetResults.get(1).get())).containsExactlyInAnyOrder(true,false);
        } finally { pool.setMaximumPoolSize(1); workers.shutdownNow(); }
    }

    @Test @Order(11)
    void publicMenuCustomerOrderHistoryAndBranchAdminApiSmoke() throws Exception {
        var customer = accounts.loadUserByUsername("a@example.com");
        var branchAdmin = accounts.loadUserByUsername("one@example.com");
        var namedSuper = accounts.loadUserByUsername("super@example.com");
        var environmentAdmin = accounts.loadUserByUsername("environment-admin");
        var driverAccount = accounts.loadUserByUsername("driver@example.com");
        String orderRequest = "{\"customerName\":\"Named Admin\",\"branchName\":\"Uitzicht\",\"items\":[{\"menuItemId\":101,\"quantity\":1}]}";
        var loginRedirect = mvc.perform(get("/order"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login")))
                .andReturn();
        var namedSuperSession = (MockHttpSession) loginRedirect.getRequest().getSession(false);
        mvc.perform(post("/login").session(namedSuperSession).with(csrf())
                        .with(request -> { request.setRemoteAddr("198.51.100.20"); return request; })
                        .param("username", "super@example.com").param("password", "admin-pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/order")));
        mvc.perform(get("/api/orders/menu").param("branch", "Kenridge"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/orders").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).with(user(customer)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"A\",\"branchName\":\"Kenridge\",\"items\":[{\"menuItemId\":101,\"quantity\":1}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").isNumber());
        mvc.perform(get("/profile/edit").with(user(customer)))
                .andExpect(status().isOk());
        mvc.perform(get("/order").session(namedSuperSession))
                .andExpect(status().isOk()).andExpect(view().name("PlaceOrder"));
        var superOrder = mvc.perform(post("/api/orders").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).session(namedSuperSession)
                        .contentType(MediaType.APPLICATION_JSON).content(orderRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").isNumber())
                .andReturn();
        long superOrderId = application.getBean(ObjectMapper.class)
                .readTree(superOrder.getResponse().getContentAsString()).path("id").asLong();
        try (Connection c = ownerConnection(); var statement = c.prepareStatement(
                "SELECT customer_id, branch_id FROM customer_order WHERE order_id=?")) {
            statement.setLong(1, superOrderId);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong("customer_id")).isEqualTo(namedSuperAdmin);
                assertThat(result.getLong("branch_id")).isEqualTo(2);
            }
        }
        mvc.perform(get("/order").with(user(environmentAdmin)))
                .andExpect(status().isOk()).andExpect(view().name("PlaceOrder"));
        var environmentOrder = mvc.perform(post("/api/orders").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).with(user(environmentAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(orderRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").isNumber())
                .andReturn();
        long environmentOrderId = application.getBean(ObjectMapper.class)
                .readTree(environmentOrder.getResponse().getContentAsString()).path("id").asLong();
        try (Connection c = ownerConnection(); var statement = c.prepareStatement("""
                SELECT o.customer_id, c.environment_admin, c.email, c.password
                FROM customer_order o JOIN customers c ON c.id = o.customer_id
                WHERE o.order_id = ?
                """)) {
            statement.setLong(1, environmentOrderId);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getBoolean("environment_admin")).isTrue();
                assertThat(result.getString("email")).isNull();
                assertThat(result.getString("password")).isNull();
            }
        }
        mvc.perform(get("/profile/edit").with(user(environmentAdmin)))
                .andExpect(status().isOk()).andExpect(view().name("customerInfoEdit"))
                .andExpect(model().attribute("readOnlyHistory", true));
        try (Connection c = ownerConnection(); var statement = c.createStatement();
             var result = statement.executeQuery("SELECT count(*) FROM customers WHERE environment_admin")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(1);
        }
        for (var blocked : List.of(branchAdmin, driverAccount)) {
            mvc.perform(get("/order").with(user(blocked))).andExpect(status().isForbidden());
            mvc.perform(post("/api/orders").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).with(user(blocked))
                    .contentType(MediaType.APPLICATION_JSON).content(orderRequest))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/admin/orders").with(user(branchAdmin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].branchId").value(1));
        mvc.perform(get("/api/admin/orders").with(user(branchAdmin)).param("branchId", "2"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(12)
    void unsafeOwnerLoginCannotStartAsRuntime() {
        SpringApplication app = new SpringApplication(OnlinePosSystemApplication.class);
        String[] arguments = Arrays.stream(runtimeArguments())
                .map(value -> value.equals("--spring.datasource.username=" + runtime)
                        ? "--spring.datasource.username=" + owner : value)
                .toArray(String[]::new);
        assertThatThrownBy(() -> {
            try (var ignored = app.run(arguments)) { /* startup must fail */ }
        }).hasRootCauseInstanceOf(IllegalStateException.class)
                .hasStackTraceContaining("Runtime database role has unsafe privileges");
    }

    @Test @Order(13)
    void existingTimestampSchemaCanUpgradeAndValidate() throws Exception {
        application.close();
        application = null;
        try (Connection c = ownerConnection(); var statement = c.createStatement()) {
            statement.execute("ALTER TABLE customer_order ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'Africa/Johannesburg'");
        }
        migrate();
        application = new SpringApplication(OnlinePosSystemApplication.class).run(runtimeArguments());
        try (Connection c = application.getBean(HikariDataSource.class).getConnection()) {
            RlsRuntimeVerifier.verify(c);
        }
    }

    private List<Long> ids(String table, String column) {
        return transaction.execute(s -> jdbc.queryForList("SELECT " + column + " FROM " + table + " ORDER BY " + column, Long.class));
    }

    private int update(String sql, Object... args) {
        Integer updated = transaction.<Integer>execute(s -> jdbc.update(sql, args));
        return updated == null ? 0 : updated;
    }

    private void as(String username) {
        SecurityContextHolder.clearContext();
        var principal = accounts.loadUserByUsername(username);
        assertThat(principal).isInstanceOf(AccountPrincipal.class);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    private void denied(Runnable action) {
        assertThatThrownBy(action::run).satisfies(failure -> {
            Throwable cause = failure;
            while (cause != null && !(cause instanceof SQLException)) cause = cause.getCause();
            assertThat(cause).isInstanceOf(SQLException.class);
            assertThat(((SQLException) cause).getSQLState()).isEqualTo("42501");
        });
    }

    private Connection adminConnection() throws SQLException { return DriverManager.getConnection(adminUrl, adminUser, adminPassword); }
    private Connection ownerConnection() throws SQLException { return DriverManager.getConnection(jdbcUrl, owner, password); }

    private String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("RLS integration tests require " + key + "; they must not be skipped");
        return value;
    }

    @AfterAll
    void cleanup() throws Exception {
        SecurityContextHolder.clearContext();
        if (application != null) application.close();
        if (adminUrl == null || adminUser == null) return;
        try (Connection c = adminConnection(); var s = c.createStatement()) {
            if (databaseCreated) s.execute("DROP DATABASE " + database + " WITH (FORCE)");
            if (rolesCreated) { s.execute("DROP ROLE " + runtime); s.execute("DROP ROLE " + owner); }
        }
    }

    private static class RlsContextInitializerForTest extends RlsContextInitializer {
        @Override public void initialize(Connection connection) throws SQLException {
            try (var statement = connection.createStatement()) {
                statement.execute("SELECT set_config('app.role','SUPER_ADMIN',true)");
            }
            throw new SQLException("Test initialization failure", "42501");
        }
    }
}
