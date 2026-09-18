# CI/CD Deployment

This project deploys to the Ubuntu server at `192.168.1.32` with GitHub Actions.
Because that address is private to the local network, deployment uses a GitHub
self-hosted runner installed on the server instead of SSH from GitHub-hosted
runners.

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

## One-Time Server Setup

SSH into the server and install the deploy helper:

```bash
ssh matthew@192.168.1.32
cd /path/to/OnlinePosSystem
DEPLOY_USER=matthew ./scripts/install-server-deploy-helper.sh
```

If the repository is not checked out on the server yet, copy the script there or
run the equivalent version from this repository.

Then add the GitHub Actions runner:

1. Open the GitHub repository.
2. Go to **Settings > Actions > Runners > New self-hosted runner**.
3. Choose **Linux** and **x64**.
4. Run GitHub's download/configure commands on `192.168.1.32`.
5. When prompted for labels, include `online-pos-system`.
6. Install and start the runner service:

```bash
sudo ./svc.sh install matthew
sudo ./svc.sh start
```

The workflow deploy job targets:

```yaml
runs-on:
  - self-hosted
  - online-pos-system
```

## GitHub Secrets

Add these secrets in GitHub under **Settings > Secrets and variables > Actions**:

```text
SPRING_DATASOURCE_PASSWORD=<database password>
JWT_SECRET=<long random secret, at least 32 characters>
```

Recommended secrets:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/online_pos_system
SPRING_DATASOURCE_USERNAME=pos_app
SERVER_PORT=8081
APP_BASE_URL=http://192.168.1.32:8081
SESSION_COOKIE_SECURE=false
RUN_MIGRATION_SQL=true
```

Optional app secrets:

```text
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<admin password>
GOOGLE_MAPS_API_KEY=<key if maps are enabled>
```

No `EC2_*` SSH secrets are needed for this private-network deployment.

## Flow

On each push to `master` or `main`, GitHub Actions:

1. Runs the test suite with the `dev` profile.
2. Builds the Spring Boot jar.
3. Uploads the jar and `staff-config.json` as a workflow artifact.
4. Runs the deploy job on the self-hosted runner at `192.168.1.32`.
5. Writes `/etc/online-pos-system/online-pos-system.env` from GitHub Secrets.
6. Installs the new jar and restarts `online-pos-system`.

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
