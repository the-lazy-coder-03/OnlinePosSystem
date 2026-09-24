# Database setup and testing

The application requires PostgreSQL 16 and separate administrator, migration-owner,
and runtime accounts. Normal runtime uses `ddl-auto=validate`, verifies private-data
RLS before JPA initialization, and never creates the schema. The `dev` profile uses
the same protection. Do not use administrator or owner credentials for runtime.

## Setup

1. Use a dedicated application database and configure the variables documented in
   `SupportConfigFiles/.env.example`.
2. Run `scripts/provision-rls.sh` as the database administrator. It transfers only
   recognized application objects and provisions separate owner/runtime logins.
3. Build the application, then run `scripts/migrate-database.sh target/OnlinePosSystem-0.0.1-SNAPSHOT.jar`
   with the owner credentials and `RLS_RUNTIME_ROLE` naming the runtime login.
4. Start the application using restricted `SPRING_DATASOURCE_*` credentials.

See [ROW_LEVEL_SECURITY.md](ROW_LEVEL_SECURITY.md) for the exact permission model,
provisioning variables and Docker Compose sequence. The active order table is
`customer_order`; the optional legacy `orders` table is default-deny. Staff PIN
login has been retired (HTTP 410). Create named admin accounts through the
super-admin interface; access levels 1 and 2 correspond to the two branches.

## Repeatable local checks

Set these variables for a disposable PostgreSQL server; each test command creates
and removes its own database and roles. Never point them at a production server.

```bash
export RLS_TEST_ADMIN_URL=jdbc:postgresql://localhost:5432/postgres
export RLS_TEST_ADMIN_USERNAME=your_local_postgres_admin
export RLS_TEST_ADMIN_PASSWORD=your_local_test_password
./scripts/test-postgres.sh -B verify -Prls-it
npm --prefix SupportConfigFiles ci
npm --prefix SupportConfigFiles exec -- playwright install chromium
npm --prefix SupportConfigFiles run test:browser
python3 scripts/check-template-scripts.py
```

The Maven command includes unit/application tests, the restricted-role RLS suite,
and packaging. Browser tests launch that packaged jar on loopback port 18081 with
a separately provisioned database. They exercise actual CSRF tokens, ordering,
profile history, private live updates and HTML-injection payloads. Test email and
maps credentials are empty; no real email delivery is attempted.

Browser session writes require the current CSRF token from the rendered page.
Bearer clients authenticate with `/api/auth/login` and send `Authorization: Bearer ...`;
bearer credentials are validated independently of any session cookie.
