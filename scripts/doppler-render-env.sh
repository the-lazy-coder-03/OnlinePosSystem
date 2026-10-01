#!/usr/bin/env bash
set -Eeuo pipefail

DOPPLER_PROJECT="${DOPPLER_PROJECT:-onlinepos}"
DOPPLER_CONFIG="${DOPPLER_CONFIG:-dev}"
ENV_FILE="${ENV_FILE:-SupportConfigFiles/.env}"

command -v doppler >/dev/null 2>&1 || {
  echo "doppler is not installed; install the Doppler CLI and run doppler login first" >&2
  exit 1
}

mkdir -p "$(dirname "$ENV_FILE")"
tmp_env="$(mktemp "${ENV_FILE}.doppler.XXXXXX")"
chmod 600 "$tmp_env"

if ! doppler secrets download \
    --no-file \
    --format docker \
    --project "$DOPPLER_PROJECT" \
    --config "$DOPPLER_CONFIG" > "$tmp_env"; then
  rm -f "$tmp_env"
  echo "Failed to render $ENV_FILE from Doppler" >&2
  exit 1
fi

mv "$tmp_env" "$ENV_FILE"
chmod 600 "$ENV_FILE"
echo "Rendered $ENV_FILE from Doppler project $DOPPLER_PROJECT config $DOPPLER_CONFIG"
