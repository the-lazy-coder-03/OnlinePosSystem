#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
compose_file="${repo_dir}/SupportConfigFiles/docker-compose.yml"
env_file="${repo_dir}/SupportConfigFiles/.env"
cert_dir="${repo_dir}/docker/certbot/conf/live/crowdcam.co.za"
challenge_dir="${repo_dir}/docker/certbot/www"
bootstrap_config="${repo_dir}/docker/nginx/certbot-bootstrap.conf"
bootstrap_container="online-pos-certbot-bootstrap"

if [[ ! -f "${env_file}" ]]; then
  echo "Missing ${env_file}. Create it from SupportConfigFiles/.env.example." >&2
  exit 1
fi

read_env_value() {
  local key="$1"
  awk -F= -v key="${key}" '$1 == key {sub(/^[^=]*=/, ""); value=$0} END {print value}' "${env_file}"
}

certbot_email="$(read_env_value CERTBOT_EMAIL)"
http_port="$(read_env_value HTTP_PORT)"
http_port="${http_port:-80}"

if [[ -z "${certbot_email}" ]]; then
  echo "CERTBOT_EMAIL must be set in SupportConfigFiles/.env." >&2
  exit 1
fi

mkdir -p "${challenge_dir}" "${repo_dir}/docker/certbot/conf"

compose=(docker compose --env-file "${env_file}" -f "${compose_file}")

if [[ -f "${cert_dir}/fullchain.pem" && -f "${cert_dir}/privkey.pem" ]]; then
  "${compose[@]}" up -d
  echo "Existing certificate found; stack started."
  exit 0
fi

"${compose[@]}" up -d db app
"${compose[@]}" stop nginx certbot-renew >/dev/null 2>&1 || true

cleanup() {
  docker stop "${bootstrap_container}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker run --rm --detach \
  --name "${bootstrap_container}" \
  -p "${http_port}:80" \
  -v "${challenge_dir}:/var/www/certbot:ro" \
  -v "${bootstrap_config}:/etc/nginx/conf.d/default.conf:ro" \
  nginx:1.27-alpine >/dev/null

"${compose[@]}" run --rm certbot certonly \
  --webroot --webroot-path /var/www/certbot \
  --cert-name crowdcam.co.za \
  --email "${certbot_email}" \
  --agree-tos --no-eff-email \
  -d crowdcam.co.za \
  -d www.crowdcam.co.za \
  -d email.crowdcam.co.za

cleanup
trap - EXIT
"${compose[@]}" up -d
echo "Certificate issued and the full stack started."
