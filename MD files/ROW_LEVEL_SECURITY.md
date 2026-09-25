# PostgreSQL row security

The normal application uses a restricted PostgreSQL login. Its request identity comes from Spring Security, then `RlsJpaDialect` initializes `app.user_id`, `app.customer_id`, `app.branch_id`, and `app.role` on the **same connection and inside each JPA transaction**. The IDs refer to the existing customer account; there is no separate user or staff account ID in the current login model. The database reads the account's current access level before setting branch and role. All four settings use `set_config(name, value, true)`: `true` makes them transaction-local, so Hikari can reuse a connection without carrying one person's identity into another person's request. Missing or invalid settings expose no protected rows.

`SQL files/rls-v1.sql` forces RLS on `customers`, `customer_order`, all eight order-line/selection tables (`order_menu_item`, `order_menu_item_extra`, `order_burger_protein`, `order_burger_removed_component`, `order_burger_extra_component`, `order_pizza_item`, `order_pizza_item_extra`, `order_pizza_item_base_option`), `staff`, `password_reset_tokens`, and `customer_notes`. The unused legacy `orders` table, if present, is preserved with default-deny RLS. Public branch, menu, pizza, and price tables remain readable without account context.

Customers can read/update their own profile and read/create their own orders and order lines. Branch admins (access levels 1 and 2) can read and update orders for their assigned branch and read full saved profiles for customers who ordered there. They can read/add notes only for those customers. Super admins (level 3 and the configured environment admin) have global administrative access. Drivers gain no order-management access. Order children derive authorization from their protected parent; ownership/branch changes are rejected by triggers. The former staff PIN/code endpoint returns HTTP 410. Named admin accounts use the existing account login, and the POS queue remains available after login.

Registration, credential lookup, and recovery cannot start with a customer RLS context. Narrow `SECURITY DEFINER` functions provide only those operations. They qualify table names, use a fixed search path, force new accounts to `USER`/level 0, and atomically consume reset tokens. Never grant runtime ownership, `BYPASSRLS`, superuser, owner-role membership, schema creation, or `TRUNCATE`; those privileges bypass or undermine RLS. The migration owner has its own forced-RLS maintenance policy and is never used by the web process. A database credential compromise or arbitrary SQL that can forge `app.*` settings remains a separate threat that requires SQL-injection defenses and credential protection.

## Local migration and rollout

1. Back up the application database. Provision or confirm a named super admin and named branch-admin accounts before switching off the legacy staff login. Keep the existing `ADMIN_USERNAME`/`ADMIN_PASSWORD` available for access assignments.
2. In a **dedicated application database**, set `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD` for a trusted database administrator. Set `MIGRATION_DATASOURCE_USERNAME`, `MIGRATION_DATASOURCE_PASSWORD`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` for two different accounts. Run `scripts/provision-rls.sh`. It transfers ownership of known application objects and creates/restricts the two logins; it does not erase data.
3. Set `MIGRATION_DATASOURCE_URL` (same database, owner credentials) and run `scripts/migrate-database.sh target/OnlinePosSystem-0.0.1-SNAPSHOT.jar`. The owner-only migration profile applies the existing catalog migration and the immutable RLS migration. The normal runtime uses `SPRING_DATASOURCE_*` and Hibernate schema validation; it refuses to start if role privileges or policies are unsafe.
4. With Docker Compose, run `docker compose --env-file SupportConfigFiles/.env -f docker/docker-compose.yml up -d db`, then `... run --rm provision`, then `... run --rm migrate`, and finally `... up -d app nginx certbot-renew`. The deployment workflow follows that order and preserves the existing TLS health check.

For isolated local PostgreSQL 16 tests, set `RLS_TEST_ADMIN_URL`, `RLS_TEST_ADMIN_USERNAME`, and `RLS_TEST_ADMIN_PASSWORD` for an administrator of a disposable PostgreSQL server, then run `./scripts/test-postgres.sh`. All Spring tests use a disposable PostgreSQL database; the RLS integration tests additionally create separate owner/runtime roles and another unique database, run migrations twice, exercise RLS through the restricted login and a one-connection Hikari pool, verify a legacy timestamp schema upgrade, and remove their database and roles afterward. Development also uses PostgreSQL: run the owner migration, then start with `SPRING_PROFILES_ACTIVE=dev` and restricted `SPRING_DATASOURCE_*` credentials. Never run the provisioning script against an unrelated shared database.

## Reviewed security contract

Startup also compares the actual definitions against `SupportConfigFiles/rls-contract.json`, packaged as
`config/rls-contract.json` inside the jar. Maven includes only this reviewed JSON
from the configuration directory; environment files and staff configuration are
not bundled. This includes all 56 policies on the 13 private tables (including
owner-maintenance policies), all 13 non-internal triggers on those tables, and the
11 `app_security` functions. Policy commands, roles, `USING`/`WITH CHECK` expressions,
function bodies, and trigger enabled states must match. Additional policies are
rejected; a policy retaining its original name cannot silently become `USING (true)`.
The optional legacy `orders` table permits only its owner-maintenance policy or no
policies. Runtime has read-only SQL grants there and sees no rows.

The verifier checks effective table/function grants as well as ownership and role
membership, and rejects PUBLIC access to private tables/functions, grant options,
database/schema creation and permission to disable triggers through
`session_replication_role`. It restores the connection's original search path after
reading the contract. The runtime test-only RLS escape hatch requires a classpath
marker that is absent from the packaged application.

The contract was generated from the reviewed migrations on a fresh PostgreSQL 16
database using `SQL files/rls-contract-query.sql` with `search_path=pg_catalog`; JSON object
ordering is irrelevant. Never regenerate it from a deployed database to silence a
startup failure. Investigate drift, restore reviewed definitions or add a new
ordered/checksummed migration, and regenerate from a fresh disposable database only
after reviewing the intended SQL changes. `SQL files/rls-v1.sql` remains immutable. This audit
changes verification without changing the installed SQL schema or policies, so no
new SQL migration is needed.

The test suite introduces weakened predicates, unexpected policies, disabled or
missing triggers, replaced functions, unsafe grants and owner memberships in isolated
transactions. It also verifies that a disabled account-protection trigger prevents
the application from starting. See [DATABASE_SETUP.md](DATABASE_SETUP.md) for Maven
and real-browser commands.
