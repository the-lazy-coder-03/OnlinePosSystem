#!/usr/bin/env bash
set -Eeuo pipefail

: "${REMOTE_APP_DIR:?REMOTE_APP_DIR is required}"
: "${DEPLOY_ACTION:?DEPLOY_ACTION must be finalize or rollback}"

PUBLIC_HOST="${PUBLIC_HOST:-crowdcam.co.za}"
ROLLBACK_TAG="onlinepossystem-app:rollback"

compose() {
  docker compose --env-file SupportConfigFiles/.env \
    -f docker/docker-compose.yml -p onlinepossystem "$@"
}

cd "$REMOTE_APP_DIR"
exec 9>.git/online-pos-deploy.lock
flock -w 1200 9 || { echo "Timed out waiting for the production deployment lock" >&2; exit 1; }

case "$DEPLOY_ACTION" in
  finalize)
    docker image rm "$ROLLBACK_TAG" >/dev/null 2>&1 || true
    echo "Production deployment finalized"
    ;;
  rollback)
    docker image inspect "$ROLLBACK_TAG" >/dev/null 2>&1 || {
      echo "The previous application image is unavailable for rollback" >&2
      exit 1
    }
    rollback_revision="$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' "$ROLLBACK_TAG")"
    docker tag "$ROLLBACK_TAG" onlinepossystem-app:latest
    compose up -d --no-deps --force-recreate --no-build app
    app_container_id="$(compose ps -q app)"
    test -n "$app_container_id"
    for _ in $(seq 1 60); do
      health_status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "$app_container_id")"
      if [[ "$health_status" == "healthy" ]] && \
          curl --fail --silent --show-error --max-time 10 \
            --resolve "${PUBLIC_HOST}:443:127.0.0.1" "https://${PUBLIC_HOST}/" >/dev/null; then
        break
      fi
      sleep 2
    done
    health_status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "$app_container_id")"
    [[ "$health_status" == "healthy" ]] || {
      echo "Rolled-back application container is not healthy: $health_status" >&2
      exit 1
    }
    docker image rm "$ROLLBACK_TAG" >/dev/null 2>&1 || true
    echo "Restored and verified previous application revision ${rollback_revision:-unknown}"
    ;;
  *)
    echo "DEPLOY_ACTION must be finalize or rollback" >&2
    exit 1
    ;;
esac
