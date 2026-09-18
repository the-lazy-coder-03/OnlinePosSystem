# CI/CD Deployment

This project deploys to the EC2 instance with GitHub Actions.

## Server

The app runs as a systemd service:

```bash
sudo systemctl status online-pos-system
sudo journalctl -u online-pos-system -f
```

Runtime environment lives at:

```bash
/etc/online-pos-system/online-pos-system.env
```

The deployed jar and staff file live at:

```bash
/opt/online-pos-system/app.jar
/opt/online-pos-system/staff-config.json
```

## GitHub Secrets

Add these secrets in GitHub under **Settings > Secrets and variables > Actions**:

```text
EC2_HOST=44.251.232.187
EC2_USER=ubuntu
EC2_SSH_KEY=<contents of your MACBOOK.pem private key>
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/online_pos_system
SPRING_DATASOURCE_USERNAME=pos_app
SPRING_DATASOURCE_PASSWORD=<database password>
JWT_SECRET=<long random secret>
```

Optional secrets:

```text
EC2_PORT=22
SERVER_PORT=8081
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin
APP_BASE_URL=http://44.251.232.187:8081
SESSION_COOKIE_SECURE=false
GOOGLE_MAPS_API_KEY=<key if maps are enabled>
RUN_MIGRATION_SQL=true
```

## Startup SQL Migration

`src/main/resources/migration.sql` can run automatically on app startup. It updates the food/menu catalog while preserving customers and customer order history.

This is enabled by default for the normal PostgreSQL app profile. If `migration.sql` changed in the commit, the app detects the new file checksum on startup and applies it once.

To disable this behavior outside GitHub Actions, set:

```text
RUN_MIGRATION_SQL=false
```

The app stores the last applied SQL checksum in `app_migration_state`. To disable automatic SQL updates on deploy, set the GitHub secret `RUN_MIGRATION_SQL=false`.

## Flow

On each push to `master` or `main`, GitHub Actions:

1. Runs the test suite with the `dev` profile.
2. Builds the Spring Boot jar.
3. Copies the jar and `staff-config.json` to EC2.
4. Writes the runtime env file from GitHub Secrets.
5. Restarts `online-pos-system`.

You can also deploy manually from the GitHub Actions tab with **Run workflow**.
