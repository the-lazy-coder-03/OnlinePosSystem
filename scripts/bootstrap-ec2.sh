#!/usr/bin/env bash
set -euo pipefail

APP_USER="${APP_USER:-onlinepos}"
APP_DIR="${APP_DIR:-/opt/online-pos-system}"
ENV_DIR="${ENV_DIR:-/etc/online-pos-system}"
SERVICE_NAME="${SERVICE_NAME:-online-pos-system}"
JAVA_PACKAGE="${JAVA_PACKAGE:-openjdk-17-jre-headless}"

SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://127.0.0.1:5432/online_pos_system}"
SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-pos_app}"
SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-}"
SERVER_PORT="${SERVER_PORT:-8081}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"
JWT_SECRET="${JWT_SECRET:-}"
SESSION_COOKIE_SECURE="${SESSION_COOKIE_SECURE:-true}"
APP_BASE_URL="${APP_BASE_URL:-https://email.crowdcam.co.za}"
RESEND_API_KEY="${RESEND_API_KEY:-${MAIL_API:-}}"
RESEND_FROM_EMAIL="${RESEND_FROM_EMAIL:-noreply@email.crowdcam.co.za}"
GOOGLE_MAPS_API_KEY="${GOOGLE_MAPS_API_KEY:-}"
RUN_MIGRATION_SQL="${RUN_MIGRATION_SQL:-true}"

if [[ -z "${SPRING_DATASOURCE_PASSWORD}" ]]; then
  echo "SPRING_DATASOURCE_PASSWORD is required" >&2
  exit 1
fi

if [[ -z "${ADMIN_PASSWORD}" ]]; then
  echo "ADMIN_PASSWORD is required" >&2
  exit 1
fi

if [[ -z "${RESEND_API_KEY}" ]]; then
  echo "RESEND_API_KEY or MAIL_API is required" >&2
  exit 1
fi

if [[ -z "${JWT_SECRET}" ]]; then
  JWT_SECRET="$(openssl rand -base64 64 | tr -d '\n')"
fi

sudo apt-get update
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y "${JAVA_PACKAGE}"

if ! id "${APP_USER}" >/dev/null 2>&1; then
  sudo useradd --system --home-dir "${APP_DIR}" --shell /usr/sbin/nologin "${APP_USER}"
fi

sudo install -d -o "${APP_USER}" -g "${APP_USER}" "${APP_DIR}"
sudo install -d -m 0750 -o root -g "${APP_USER}" "${ENV_DIR}"

tmp_env="$(mktemp)"
cat > "${tmp_env}" <<EOF
SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}
SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME}
SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}
SERVER_ADDRESS=0.0.0.0
SERVER_PORT=${SERVER_PORT}
SPRING_PROFILES_ACTIVE=default
ADMIN_USERNAME=${ADMIN_USERNAME}
ADMIN_PASSWORD=${ADMIN_PASSWORD}
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION_SECONDS=7200
SESSION_COOKIE_SECURE=${SESSION_COOKIE_SECURE}
APP_BASE_URL=${APP_BASE_URL}
RESEND_API_KEY=${RESEND_API_KEY}
RESEND_FROM_EMAIL=${RESEND_FROM_EMAIL}
GOOGLE_MAPS_API_KEY=${GOOGLE_MAPS_API_KEY}
RUN_MIGRATION_SQL=${RUN_MIGRATION_SQL}
STAFF_CONFIG_PATH=${APP_DIR}/staff-config.json
EOF

sudo install -m 0640 -o root -g "${APP_USER}" "${tmp_env}" "${ENV_DIR}/${SERVICE_NAME}.env"
rm -f "${tmp_env}"

tmp_service="$(mktemp)"
cat > "${tmp_service}" <<EOF
[Unit]
Description=Online POS System
After=network-online.target postgresql.service
Wants=network-online.target

[Service]
Type=simple
User=${APP_USER}
Group=${APP_USER}
WorkingDirectory=${APP_DIR}
EnvironmentFile=${ENV_DIR}/${SERVICE_NAME}.env
ExecStart=/usr/bin/java -jar ${APP_DIR}/app.jar
Restart=always
RestartSec=10
SuccessExitStatus=143
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF

sudo install -m 0644 -o root -g root "${tmp_service}" "/etc/systemd/system/${SERVICE_NAME}.service"
rm -f "${tmp_service}"

sudo systemctl daemon-reload
sudo systemctl enable "${SERVICE_NAME}"

if [[ -f "${APP_DIR}/app.jar" ]]; then
  sudo systemctl restart "${SERVICE_NAME}"
else
  echo "Service installed. Deploy app.jar to ${APP_DIR}/app.jar before starting ${SERVICE_NAME}."
fi

echo "Bootstrap complete for ${SERVICE_NAME}."
