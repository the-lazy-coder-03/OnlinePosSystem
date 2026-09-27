# PostgreSQL row security

The normal application uses the restricted `pos_runtime` PostgreSQL login. Spring Security supplies a trusted `AccountPrincipal`, and `RlsJpaDialect` initializes `app.customer_id` and `app.context_signature` on the same connection and inside each JPA transaction. The HMAC signature uses `RLS_CONTEXT_SECRET` and is bound to the customer ID, PostgreSQL backend PID, transaction ID, and context version. Transaction-local settings prevent Hikari connection reuse from carrying identity into another request.

The database derives the actor role and branch from the protected customer row and its current `access_level`. Values supplied through `app.role`, `app.user_id`, and `app.branch_id` are never authorization inputs. Missing, malformed, incorrectly signed, or replayed contexts expose no protected rows. The environment administrator resolves through its protected `environment_admin` customer row and receives the same signed context as named accounts.

The immutable migrations force RLS on `customers`, `customer_order`, `customer_notes`, `staff`, `password_reset_tokens`, and every order-line or customization table. The optional legacy `orders` table remains default-deny. Shared branch, menu, burger, pizza, ingredient, price, modifier, and specials catalog tables have RLS disabled and remain readable without customer context. Runtime catalog writes retain only the SQL privileges used by the existing Spring-authorized super-admin services.

Customers can read and update their profile and read or create their own orders and order lines. Branch admins can read their branch orders and children, update branch order headers, and read or add notes for customers who ordered at that branch. Super admins have global administrative access. Every nested order predicate joins through its complete parent chain to `customer_order`. Triggers reject customer, branch, order, line-item, and special-selection re-parenting.

Registration, credential lookup, and password recovery use narrow `SECURITY DEFINER` functions because they begin without a customer RLS context. These functions use qualified names and a fixed `pg_catalog, pg_temp` search path. Runtime has no direct access to `password_reset_tokens` or the owner-only `app_security.rls_context_secret` table. Function execution is an explicit allowlist, and `PUBLIC` receives no access to `app_security` functions.

Never grant the runtime role object ownership, `BYPASSRLS`, superuser, owner-role membership, database/schema creation, `TRUNCATE`, or grant options. The migration owner is separate from the web runtime and has owner-only maintenance policies on forced-RLS tables.

## Local migration and rollout

1. Back up the application database.
2. Generate one random `RLS_CONTEXT_SECRET` containing at least 32 characters. Configure the same value for the migration process and every runtime instance. Never commit or log it.
3. In a dedicated application database, set `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD` for a trusted database administrator. Set `MIGRATION_DATASOURCE_USERNAME`, `MIGRATION_DATASOURCE_PASSWORD`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` for separate owner and runtime accounts.
4. Run `scripts/provision-rls.sh`. It installs `pgcrypto`, transfers ownership of recognized application objects, and creates or restricts the owner/runtime logins without erasing data.
5. Build the application, set `MIGRATION_DATASOURCE_URL`, and run `scripts/migrate-database.sh target/OnlinePosSystem-0.0.1-SNAPSHOT.jar`. The migration profile applies the catalog and ordered RLS migrations, then synchronizes the signing key through a prepared statement.
6. Start every application instance with restricted `SPRING_DATASOURCE_*` credentials and the same `RLS_CONTEXT_SECRET`. Startup fails if the key differs from the database fingerprint or if roles, grants, policies, triggers, or function definitions drift.

With Docker Compose, start `db`, run the one-shot `provision` service, run `migrate`, and then start `app`, `nginx`, and `certbot-renew`. The deploy workflow follows the same order.

For isolated PostgreSQL 16 tests, set `RLS_TEST_ADMIN_URL`, `RLS_TEST_ADMIN_USERNAME`, and `RLS_TEST_ADMIN_PASSWORD`, then run `./scripts/test-postgres.sh -B verify -Prls-it`. The suite creates separate owner/runtime roles and a disposable database, applies the immutable migrations twice, exercises a one-connection Hikari pool, and removes its database and roles afterward.

## Reviewed security contract

Startup compares the database definitions with `SupportConfigFiles/rls-contract.json`, packaged as `config/rls-contract.json`. The current contract includes 71 policies, 17 non-internal triggers, and 17 `app_security` functions. It records policy commands, target roles, `USING` and `WITH CHECK` expressions, trigger definitions and enabled states, and complete function definitions.

The verifier also rejects catalog RLS, missing forced RLS, unsafe effective table/function grants, `PUBLIC` access, grant options, secret-table access, owner membership, `BYPASSRLS`, database/schema creation, and permission to disable triggers. It checks a domain-separated HMAC fingerprint so an application configured with the wrong signing secret cannot start.

The contract is generated from reviewed migrations on a fresh PostgreSQL 16 database using `SQL files/rls-contract-query.sql` with `search_path=pg_catalog`. Never regenerate it from a deployed database merely to silence a startup failure. Investigate drift, restore reviewed definitions or add a new ordered migration, then regenerate from a fresh disposable database. `SQL files/rls-v1.sql` and `SQL files/rls-v2-specials.sql` remain immutable; hardening lives in ordered v3 and v4 migrations.

The integration tests also introduce policy, trigger, function, grant, role, and signing-key failures and verify that startup rejects them. See [DATABASE_SETUP.md](DATABASE_SETUP.md) for the full test commands.
