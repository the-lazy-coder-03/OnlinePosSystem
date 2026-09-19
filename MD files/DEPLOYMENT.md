# CI/CD Deployment

The live `crowdcam.co.za` website runs with Docker Compose on `130.131.162.110`
in `/home/azureuser/OnlinePosSystem`. Its runtime settings come from that
directory's `SupportConfigFiles/.env`. The proxy on `40.76.227.74` forwards website traffic there.
The systemd deployment described below is separate from this live Docker app.

This project deploys to the Azure Ubuntu VM at `40.76.227.74` with GitHub Actions.
The workflow builds on a GitHub-hosted runner, uploads the release over SSH, and
restarts the `online-pos-system` systemd service on the VM.

## Server Layout

The app runs as a systemd service:

```bash
sudo systemctl status online-pos-system
sudo journalctl -u online-pos-system -f
```

Runtime files:

```text
/etc/online-pos-system/online-pos-system.env
/opt/online-pos-system/app.jar
/opt/online-pos-system/staff-config.json
/usr/local/sbin/deploy-online-pos-system
```

The Azure VM has PostgreSQL installed locally. The app database is:

```text
Database: online_pos_system
User: pos_app
Host: 127.0.0.1
Port: 5432
```

## One-Time Server Setup

The VM needs Java, PostgreSQL, the `online-pos-system` service, and the deploy
helper. From this repository, run:

```bash
scp -i ~/Downloads/teszt_key.pem scripts/install-server-deploy-helper.sh azureuser@40.76.227.74:/tmp/install-server-deploy-helper.sh
ssh -i ~/Downloads/teszt_key.pem azureuser@40.76.227.74 \
  'chmod +x /tmp/install-server-deploy-helper.sh && DEPLOY_USER=azureuser /tmp/install-server-deploy-helper.sh'
```

## GitHub Secrets

Add these secrets in GitHub under **Settings > Secrets and variables > Actions**:

```text
AZURE_VM_HOST=40.76.227.74
AZURE_VM_USER=azureuser
AZURE_VM_PORT=22
AZURE_VM_SSH_KEY=<private deploy key for GitHub Actions>
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/online_pos_system
SPRING_DATASOURCE_USERNAME=pos_app
SPRING_DATASOURCE_PASSWORD=<database password>
JWT_SECRET=<long random secret, at least 32 characters>
SERVER_PORT=8081
APP_BASE_URL=https://email.crowdcam.co.za
SESSION_COOKIE_SECURE=true
RUN_MIGRATION_SQL=true
```

Optional app secrets:

```text
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<admin password>
GOOGLE_MAPS_API_KEY=<key if maps are enabled>
RESEND_API_KEY=<Resend sending_access API key>
RESEND_FROM_EMAIL=noreply@email.crowdcam.co.za
```

## Flow

On each push to `master` or `main`, GitHub Actions:

1. Runs the test suite with the `dev` profile.
2. Builds the Spring Boot jar.
3. Uploads the jar and `staff-config.json` as a workflow artifact.
4. Connects to `azureuser@40.76.227.74` over SSH.
5. Writes `/etc/online-pos-system/online-pos-system.env` from GitHub Secrets.
6. Installs the new jar and restarts `online-pos-system`.
7. Runs a local health check against `http://127.0.0.1:8081/` on the VM.

You can also deploy manually from the GitHub Actions tab with **Run workflow**.

## Startup SQL Migration

`src/main/resources/migration.sql` can run automatically on app startup. It
updates the food/menu catalog while preserving customers and customer order
history.

This is enabled by default for the normal PostgreSQL app profile. If
`migration.sql` changed in the commit, the app detects the new file checksum on
startup and applies it once.

To disable this behavior, set the GitHub secret:

```text
RUN_MIGRATION_SQL=false
```

The app stores the last applied SQL checksum in `app_migration_state`.

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
  -f SupportConfigFiles/docker-compose.yml up -d --no-deps --force-recreate app
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
