#!/usr/bin/env bash
set -Eeuo pipefail

DOPPLER_PROJECT="${DOPPLER_PROJECT:-onlinepos}"
DOPPLER_CONFIG="${DOPPLER_CONFIG:-dev}"
ENV_FILE="${ENV_FILE:-SupportConfigFiles/.env}"
CONFIRM_PRD_UPLOAD="${CONFIRM_PRD_UPLOAD:-}"

command -v doppler >/dev/null 2>&1 || {
  echo "doppler is not installed; install the Doppler CLI and run doppler login first" >&2
  exit 1
}

test -f "$ENV_FILE" || {
  echo "Environment file not found: $ENV_FILE" >&2
  exit 1
}

if [[ "$DOPPLER_CONFIG" == "prd" && "$CONFIRM_PRD_UPLOAD" != "${DOPPLER_PROJECT}/prd" ]]; then
  echo "Refusing to upload to production without CONFIRM_PRD_UPLOAD=${DOPPLER_PROJECT}/prd" >&2
  echo "Set ENV_FILE to a production-safe env file before uploading prd secrets." >&2
  exit 1
fi

tmp_log="$(mktemp "${TMPDIR:-/tmp}/doppler-upload.XXXXXX")"
chmod 600 "$tmp_log"
cleanup() {
  if [[ -e "$tmp_log" ]]; then
    rm "$tmp_log"
  fi
}
trap cleanup EXIT

doppler secrets upload \
  --project "$DOPPLER_PROJECT" \
  --config "$DOPPLER_CONFIG" \
  "$ENV_FILE" > "$tmp_log"

echo "Uploaded $ENV_FILE to Doppler project $DOPPLER_PROJECT config $DOPPLER_CONFIG."
echo "Required secret presence:"
doppler secrets download \
  --no-file \
  --format docker \
  --project "$DOPPLER_PROJECT" \
  --config "$DOPPLER_CONFIG" \
  | grep -E '^(SPRING_DATASOURCE_PASSWORD|JWT_SECRET|RLS_CONTEXT_SECRET|GOOGLE_MAPS_API_KEY)=' \
  | sed -E 's/=.*/=<present>/'
