# CI/CD deployment

The repository workflow deploys a Docker Compose stack using the configured
`AZURE_VM_*` secrets and `REMOTE_APP_DIR`. Treat historical server addresses as
inventory requiring verification, not a source of deployment configuration.

## Verification and deployment flow

Pull requests and pushes to `master`/`main` run Java 17 Maven verification against
PostgreSQL 16, including the dedicated RLS integration profile. The workflow then
runs Chromium browser regressions and script syntax checks. Pull requests never
run the deployment job. Pushes to those branches and manual workflow dispatches
retain the existing deployment behavior after all build checks pass.

Deployment checks out the tested revision, builds images, provisions database
roles, runs owner migrations, and recreates the runtime application container.
It checks container health, the image revision, and the existing HTTPS endpoint.
The application image can be rolled back on failure; that rollback does not undo
database changes. Back up the database before applying schema migrations.

## Credentials and schema ownership

Configure `POSTGRES_USER`/`POSTGRES_PASSWORD` for the database administrator,
`MIGRATION_DATASOURCE_USERNAME`/`MIGRATION_DATASOURCE_PASSWORD` for the owner,
and `SPRING_DATASOURCE_USERNAME`/`SPRING_DATASOURCE_PASSWORD` for runtime.
These must be three separate accounts. See `SupportConfigFiles/.env.example` and
[ROW_LEVEL_SECURITY.md](ROW_LEVEL_SECURITY.md) for provisioning and migration steps.

The normal application never migrates its schema. Only the separate `migrate`
profile runs `migration.sql` and the immutable RLS migration. `RUN_MIGRATION_SQL`
controls the catalog migration in that profile; it does not disable runtime RLS.
Never edit an applied immutable migration. Add an ordered migration when changing
SQL security definitions and update the reviewed security contract alongside it.

The older systemd installer remains a separate deployment helper. It only installs
and restarts a jar: its database must already be provisioned and migrated, and its
runtime environment must contain restricted credentials. It is not the workflow's
current deployment path.

## Browser and proxy security

`APP_BASE_URL` supplies the default allowed CORS origin. If separate trusted browser
origins need API access, configure the comma-separated Spring property
`app.cors.allowed-origins` (`APP_CORS_ALLOWED_ORIGINS`); credentialed wildcard origins
are rejected. Same-origin requests continue to work normally.

Forwarded headers are handled by Tomcat's native trusted-proxy support. Configure
`server.tomcat.remoteip.internal-proxies`/`trusted-proxies` for the actual proxy
network when necessary. Application rate limiting uses the resolved remote address,
not a client-supplied raw `X-Forwarded-For` value. Keep secure session cookies enabled
on HTTPS deployments; local browser tests explicitly disable the secure flag.

## Password Reset Email

Password reset uses the official `com.resend:resend-java` SDK to send a one-time link to
`/reset-password?token=...`. The token expires after 30 minutes, is stored only
as a SHA-256 hash, and is invalidated after a successful reset.

Required environment values:

```text
APP_BASE_URL=https://email.crowdcam.co.za
RESEND_API_KEY=<Resend sending_access API key>
RESEND_FROM_EMAIL=noreply@email.crowdcam.co.za
RUN_MIGRATION_SQL=true
```

The main application remains at `https://crowdcam.co.za`. `APP_BASE_URL` uses
`https://email.crowdcam.co.za` so emailed reset links open on the dedicated
reset hostname. The same verified domain belongs in `RESEND_FROM_EMAIL`.

The SDK sends through `https://api.resend.com`; the former `RESEND_ENDPOINT`
override is no longer used. Accepted emails log their Resend email ID, while
rejections log the provider status and error type without credentials or reset links.

Use `RESEND_API_KEY`; `MAIL_API` remains a fallback for older installations.
A `sending_access` key is sufficient. The application does not list domains
or validate credentials through account-management endpoints. Actuator's
`resend` health component checks configuration presence only; it does not
verify key validity, domain status, delivery, or send test emails.
Ensure `email.crowdcam.co.za` is verified in Resend and the sending key is
allowed to send from that domain.
After changing the live `.env`, recreate the app container to apply it:

```bash
docker compose --env-file SupportConfigFiles/.env \
  -f docker/docker-compose.yml up -d --no-deps --force-recreate app
```

## Docker Compose and HTTPS

Set `CERTBOT_EMAIL` in `SupportConfigFiles/.env`, point `crowdcam.co.za`,
`www.crowdcam.co.za`, and `email.crowdcam.co.za` at the Compose server, and
open inbound ports 80 and 443. PostgreSQL port 5432 remains internal.

Issue the initial certificate and start the stack with:

```bash
scripts/init-letsencrypt.sh
```

The certificate includes all three hostnames. The renewal container checks
twice daily and Nginx reloads certificates periodically. The main host proxies
the full application; the email host serves password-reset and static asset
requests and redirects other paths to `https://crowdcam.co.za`.
