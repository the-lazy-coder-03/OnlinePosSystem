#!/usr/bin/env bash
set -euo pipefail

APP_USER="${APP_USER:-onlinepos}"
APP_DIR="${APP_DIR:-/opt/online-pos-system}"
ENV_DIR="${ENV_DIR:-/etc/online-pos-system}"
SERVICE_NAME="${SERVICE_NAME:-online-pos-system}"
DEPLOY_USER="${DEPLOY_USER:-${SUDO_USER:-$(id -un)}}"
JAVA_PACKAGE="${JAVA_PACKAGE:-openjdk-17-jre-headless}"

run_apt() {
  local attempt=1
  local max_attempts="${APT_MAX_ATTEMPTS:-90}"

  until sudo DEBIAN_FRONTEND=noninteractive "$@"; do
    if (( attempt >= max_attempts )); then
      echo "apt command failed after ${max_attempts} attempts: $*" >&2
      exit 1
    fi

    echo "apt is busy or failed; retrying in 10 seconds (${attempt}/${max_attempts})..." >&2
    attempt=$((attempt + 1))
    sleep 10
  done
}

if ! id "${DEPLOY_USER}" >/dev/null 2>&1; then
  echo "DEPLOY_USER '${DEPLOY_USER}' does not exist" >&2
  exit 1
fi

case "${DEPLOY_USER}" in
  *[!a-zA-Z0-9_.$-]* | "")
    echo "DEPLOY_USER '${DEPLOY_USER}' is not a safe sudoers username" >&2
    exit 1
    ;;
esac

run_apt apt-get update
run_apt apt-get install -y "${JAVA_PACKAGE}"

if ! id "${APP_USER}" >/dev/null 2>&1; then
  sudo useradd --system --home-dir "${APP_DIR}" --shell /usr/sbin/nologin "${APP_USER}"
fi

sudo install -d -o "${APP_USER}" -g "${APP_USER}" "${APP_DIR}"
sudo install -d -m 0750 -o root -g "${APP_USER}" "${ENV_DIR}"

tmp_helper="$(mktemp)"
cat > "${tmp_helper}" <<'HELPER'
#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: deploy-online-pos-system <app.jar> <staff-config.json> <online-pos-system.env>" >&2
  exit 64
fi

APP_USER="${APP_USER:-onlinepos}"
APP_DIR="${APP_DIR:-/opt/online-pos-system}"
ENV_DIR="${ENV_DIR:-/etc/online-pos-system}"
SERVICE_NAME="${SERVICE_NAME:-online-pos-system}"

jar_path="$1"
staff_config_path="$2"
env_path="$3"

for path in "${jar_path}" "${staff_config_path}" "${env_path}"; do
  if [[ ! -f "${path}" ]]; then
    echo "Missing deployment file: ${path}" >&2
    exit 66
  fi
done

if ! id "${APP_USER}" >/dev/null 2>&1; then
  useradd --system --home-dir "${APP_DIR}" --shell /usr/sbin/nologin "${APP_USER}"
fi

install -d -o "${APP_USER}" -g "${APP_USER}" "${APP_DIR}"
install -d -m 0750 -o root -g "${APP_USER}" "${ENV_DIR}"
install -m 0640 -o root -g "${APP_USER}" "${env_path}" "${ENV_DIR}/${SERVICE_NAME}.env"
install -m 0644 -o "${APP_USER}" -g "${APP_USER}" "${staff_config_path}" "${APP_DIR}/staff-config.json"
install -m 0644 -o "${APP_USER}" -g "${APP_USER}" "${jar_path}" "${APP_DIR}/app.jar"

systemctl daemon-reload
systemctl restart "${SERVICE_NAME}"
systemctl is-active --quiet "${SERVICE_NAME}"
HELPER

sudo install -m 0755 -o root -g root "${tmp_helper}" /usr/local/sbin/deploy-online-pos-system
rm -f "${tmp_helper}"

tmp_service="$(mktemp)"
cat > "${tmp_service}" <<EOF
[Unit]
Description=Online POS System
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=${APP_USER}
Group=${APP_USER}
WorkingDirectory=${APP_DIR}
EnvironmentFile=${ENV_DIR}/${SERVICE_NAME}.env
ExecStartPre=/usr/bin/test -f ${APP_DIR}/app.jar
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

tmp_sudoers="$(mktemp)"
cat > "${tmp_sudoers}" <<EOF
# Allow the GitHub Actions runner user to activate Online POS releases.
${DEPLOY_USER} ALL=(root) NOPASSWD: /usr/local/sbin/deploy-online-pos-system *
EOF

sudo visudo -cf "${tmp_sudoers}" >/dev/null
sudo install -m 0440 -o root -g root "${tmp_sudoers}" "/etc/sudoers.d/${SERVICE_NAME}-deploy"
rm -f "${tmp_sudoers}"

sudo systemctl daemon-reload
sudo systemctl enable "${SERVICE_NAME}"

echo "Installed ${SERVICE_NAME} service and deploy helper."
echo "GitHub Actions runner user '${DEPLOY_USER}' can run /usr/local/sbin/deploy-online-pos-system via sudo without a password."
