# Project audit — 24 September 2026

Implemented and verified against disposable local PostgreSQL 16 databases. No
production database inspection, schema change or deployment was performed.

## Changes

| Area | Finding and correction |
| --- | --- |
| Private-data RLS | Startup previously checked policy names without validating their meaning. It now checks the shipped contract for policy commands/roles/predicates, enabled triggers and helper-function definitions, together with effective role/table/function privileges. Unexpected policies and dangerous grants prevent startup. |
| Browser API security | Replaced the blanket `/api/**` CSRF exemption with validated bearer authentication and narrow public JSON authentication/recovery exceptions. Updated pizza quotes, order placement and POS status writes to send the rendered CSRF token. |
| Bearer authentication | Bearer requests authenticate independently of cookies, use a separate security context, reject invalid/deleted-account credentials, and do not overwrite the browser session. |
| CORS and proxy identity | Removed credentialed access from arbitrary origins. Allowed origins default to the configured application origin; Tomcat handles trusted-proxy forwarding and rate limits use the resolved remote address. |
| Registration | Direct sign-in after registration now rotates the anonymous session ID and clears its old CSRF token. |
| HTML rendering | Customer, address, item and other order values use DOM text nodes in both POS views. Status classes use an allowlist. Catalog category names are HTML-escaped. |
| WebSocket access | Reject client publishing and unknown subscriptions. Preserve authenticated customer private queues and branch/super-admin order subscriptions. |
| Price validation | Single and optional price inputs now reject NaN, infinity, negative values, excessive precision and out-of-range amounts consistently with bulk updates. |
| Dependencies | Updated Spring Boot from 3.2.2 to 3.5.16 and its managed dependency set; removed a duplicate test JSON implementation. Reduced normal Spring Security logging to INFO. |
| Verification and documentation | Added pull-request verification without enabling pull-request deployment, Chromium regression tests, embedded-script parsing, and current database/deployment instructions. Provisioning accepts explicit database connection URIs as well as database names. |

Spring Boot 3.5.16 retains Java 17 support and supports Java 25; see the
[official system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
The CSRF changes follow [Spring Security's browser protection model](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
The RLS checks account for [PostgreSQL's permissive policy composition and bypass privileges](https://www.postgresql.org/docs/16/ddl-rowsecurity.html).

## RLS inventory and access paths

The security contract covers 13 private tables: `customers`, `customer_notes`,
`password_reset_tokens`, `staff`, `customer_order`, `order_menu_item`,
`order_menu_item_extra`, `order_burger_protein`, `order_burger_removed_component`,
`order_burger_extra_component`, `order_pizza_item`, `order_pizza_item_extra`, and
`order_pizza_item_base_option`. The optional legacy `orders` table is checked
separately for default-deny behavior. The contract records 56 policies, 13
non-internal triggers and 11 security helper functions.

Customer/order/note operations execute within JPA transactions. Both private JDBC
adapters (`PostgresAccountBootstrapStore` and `EnvironmentAdminAccountService`) are
transactional. `RlsJpaDialect` initializes identity on the same connection, using
transaction-local settings and the account's current database access level.
Authentication/registration/recovery use the reviewed, narrowly scoped definer
functions. Realtime listeners publish committed DTOs rather than querying private
rows with a missing asynchronous principal; WebSocket subscription authorization
protects delivery.

Catalog/branch/pricing data retain their existing public-read and application-level
write permissions, as requested. Customer ownership, branch-admin visibility,
super-admin access, driver restrictions and note visibility retain their existing
semantics. Applied `rls-v1.sql` and `migration.sql` are unchanged; no new SQL migration
is required for these verification and application changes.

## Verification results

- **193 regular Java tests and 16 PostgreSQL RLS integration tests passed on Java 17.0.18**, with successful Maven packaging under Spring Boot 3.5.16. The core suite also passed on Java 25 before the final price-validation additions.
- The RLS suite exercises 46 deliberately unsafe policy/trigger/function/grant configurations, owner-login rejection, application startup with a disabled protection trigger, transaction/pool isolation, customer/branch permissions, concurrent recovery consumption and repeat migrations/legacy timestamp compatibility.
- **4 Chromium tests passed** against the packaged application with separate administrator, owner and runtime credentials. They cover malicious HTML rendering, real pizza ordering and quoting, private live admin updates, order status changes, profile history, registration/session rotation, profile saving, public pages, CSRF rejection and invalid bearer rejection. The successful order/live-queue flow reports no browser console or JavaScript errors.
- All shell scripts and standalone JavaScript passed syntax checks. All six embedded JavaScript blocks across the templates parsed successfully.
- `npm --prefix SupportConfigFiles audit --audit-level=high` reported zero vulnerabilities in the browser-test dependency tree. This is not a comprehensive Maven dependency vulnerability scan.
- The runtime jar contains no `postgres-test-only.marker`. Original SQL migration files are unchanged. Test databases and roles are cleaned up by the harnesses, and `git diff --check` passes.

The transaction-event regression harness now explicitly enables transaction event
support and checks rollback as well as commit; its previous setup depended on
asynchronous timing rather than actually registering the transaction listener
factory.

## Limits and operational compatibility

Repository inspection and automated checks cover authentication, recovery,
profiles, catalog/customization, orders, administration, private persistence,
realtime delivery, templates, configuration and deployment scripts. These checks
establish tested behavior, not proof that every possible input or concurrency
interleaving is free of defects.

Docker image builds, remote proxy/TLS behavior, real email/maps services, production
data, and actual deployed database privileges were not exercised. CI is configured
to run the Java 17/PostgreSQL/Chromium checks, but no remote workflow was dispatched.

The RLS contract uses PostgreSQL 16's canonical definitions. Intentional policy,
function or trigger changes must be reviewed and shipped with an updated contract
and a new ordered/checksummed migration when SQL changes are needed. Do not replace
the contract with a snapshot from a deployed database merely to bypass a startup
failure. Runtime credentials and application-issued `app.*` settings remain the
trust boundary; RLS does not authenticate arbitrary SQL supplied with stolen
runtime credentials.

Browser clients must send CSRF tokens for session-based mutations. Cross-origin
browser clients require an explicitly configured trusted origin. Existing bearer
API callers continue using the Authorization header, with invalid credentials
returning 401 even when a valid session cookie is also present.

See [DATABASE_SETUP.md](DATABASE_SETUP.md) for repeatable test commands and
[ROW_LEVEL_SECURITY.md](ROW_LEVEL_SECURITY.md) for the security contract and role model.
