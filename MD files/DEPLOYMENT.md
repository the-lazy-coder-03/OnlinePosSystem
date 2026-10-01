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

## Doppler secrets

Doppler is the source of truth for runtime application secrets. GitHub Actions
keeps only SSH deployment secrets plus CI-only test placeholders. Production
app, database, email, JWT, RLS, and Google Maps values belong in the Doppler
project `onlinepos`, config `prd`; local development uses config `dev`.

Install the Doppler CLI on macOS, then sign in and select the project/config:

```bash
brew install gnupg
brew install dopplerhq/cli/doppler
doppler --version
doppler login
doppler setup --project onlinepos --config dev
```

Import the current ignored `SupportConfigFiles/.env` once for local development,
then treat Doppler as canonical. Use the helper instead of raw
`doppler secrets upload` because Doppler prints uploaded values by default:

```bash
scripts/doppler-upload-env.sh
```

To upload production values, use a production-safe env file and explicitly
confirm the `prd` target:

```bash
ENV_FILE=/path/to/production.env \
DOPPLER_CONFIG=prd \
CONFIRM_PRD_UPLOAD=onlinepos/prd \
scripts/doppler-upload-env.sh
```

Render a Docker Compose-compatible env file for local commands:

```bash
scripts/doppler-render-env.sh
docker compose --env-file SupportConfigFiles/.env -f docker/docker-compose.yml config
```

Install the Doppler CLI on the Debian/Ubuntu VM with Doppler's signed apt
repository:

```bash
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl gnupg
curl -sLf --retry 3 --tlsv1.2 --proto '=https' \
  'https://packages.doppler.com/public/cli/gpg.DE2A7741A397C129.key' \
  | sudo gpg --dearmor -o /usr/share/keyrings/doppler-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/doppler-archive-keyring.gpg] https://packages.doppler.com/public/cli/deb/debian any-version main" \
  | sudo tee /etc/apt/sources.list.d/doppler-cli.list
sudo apt-get update
sudo apt-get install -y doppler
doppler --version
```

Create a read-only Doppler Service Token scoped only to
`onlinepos/prd`; do not use a personal token on the VM. Configure it
for the application directory:

```bash
echo '<production-service-token>' \
  | doppler configure set token --scope /home/<azure-user>/OnlinePosSystem
doppler configure set project onlinepos --scope /home/<azure-user>/OnlinePosSystem
doppler configure set config prd --scope /home/<azure-user>/OnlinePosSystem
```

The deployment script refreshes `SupportConfigFiles/.env` from Doppler before
`docker compose up`, `provision`, and `migrate`. To verify the VM without
printing secret values, run:

```bash
cd /home/<azure-user>/OnlinePosSystem
doppler secrets download --no-file --format docker \
  --project onlinepos --config prd \
  | grep -E '^(SPRING_DATASOURCE_PASSWORD|JWT_SECRET|RLS_CONTEXT_SECRET|GOOGLE_MAPS_API_KEY)='
```

For a non-deploy dry check after rendering the env file:

```bash
doppler secrets download --no-file --format docker \
  --project onlinepos --config prd > SupportConfigFiles/.env
chmod 600 SupportConfigFiles/.env
docker compose --env-file SupportConfigFiles/.env -f docker/docker-compose.yml config
```

## Credentials and schema ownership

Configure `POSTGRES_USER`/`POSTGRES_PASSWORD` for the database administrator,
`MIGRATION_DATASOURCE_USERNAME`/`MIGRATION_DATASOURCE_PASSWORD` for the owner,
and `SPRING_DATASOURCE_USERNAME`/`SPRING_DATASOURCE_PASSWORD` for runtime.
These must be three separate accounts. See `SupportConfigFiles/.env.example` and
[ROW_LEVEL_SECURITY.md](ROW_LEVEL_SECURITY.md) for provisioning and migration steps.

The normal application never migrates its schema. Only the separate `migrate`
profile runs `SQL files/migration.sql` and the immutable RLS migration. `RUN_MIGRATION_SQL`
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
GOOGLE_MAPS_API_KEY=<restricted browser key for Maps JavaScript and Places API (New)>
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

`GOOGLE_MAPS_API_KEY` is rendered into registration, profile and checkout pages
so the browser can load the Maps JavaScript Places autocomplete data API. The
application requests South African predictions after two typed characters and
renders the suggestions in its own accessible address list. Enable billing,
Maps JavaScript API and Places API (New) for that key. Restrict it in Google
Cloud by HTTP referrer to `https://crowdcam.co.za/*` and
`https://www.crowdcam.co.za/*` (plus explicit local development origins when
needed), and restrict API usage to those two APIs. Do not commit a real key. Browser-provided place IDs,
coordinates and formatted addresses are stored as address metadata only; any
future delivery fee or serviceability decision must be recalculated by the
server using trusted provider data.

After changing the key, open `/register`, type at least two characters of a
generic South African street address, and confirm that suggestions appear below
the field and populate the structured address fields when selected. Check the
browser console for Google authentication errors. `API Key not found` means the
deployed key is missing, invalid, or not authorized for the request; also verify
billing, both enabled APIs, the API restriction list, and the exact HTTP
referrers before recreating the application container.

After changing Doppler `prd` secrets, refresh the generated env file and recreate
the app container to apply them:

```bash
cd /home/<azure-user>/OnlinePosSystem
doppler secrets download --no-file --format docker \
  --project onlinepos --config prd > SupportConfigFiles/.env
chmod 600 SupportConfigFiles/.env
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
