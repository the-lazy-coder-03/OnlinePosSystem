#!/usr/bin/env bash
set -Eeuo pipefail

: "${REMOTE_APP_DIR:?REMOTE_APP_DIR is required}"
: "${DEPLOY_BRANCH:?DEPLOY_BRANCH is required}"
: "${DEPLOY_SHA:?DEPLOY_SHA is required}"
: "${RUNTIME_ENV_CREATED:?RUNTIME_ENV_CREATED is required}"
: "${REMOTE_ENV:?REMOTE_ENV is required}"

PUBLIC_HOST="${PUBLIC_HOST:-crowdcam.co.za}"
ROLLBACK_TAG="onlinepossystem-app:rollback"
OLD_CONTAINER_ID=""
OLD_IMAGE_ID=""
ROLLBACK_AVAILABLE=false
APP_RECREATED=false

cleanup() {
  case "$0" in
    /tmp/online-pos-system-deploy-*.sh) rm -f -- "$0" ;;
  esac
}
trap cleanup EXIT

compose() {
  docker compose --env-file SupportConfigFiles/.env \
    -f docker/docker-compose.yml -p onlinepossystem "$@"
}

rollback() {
  local status=$?
  trap - ERR
  set +e
  echo "Deployment failed; restoring the previous application container." >&2

  if [[ "$APP_RECREATED" == "true" && "$ROLLBACK_AVAILABLE" == "true" ]]; then
    docker tag "$ROLLBACK_TAG" onlinepossystem-app:latest
    compose up -d --no-deps --force-recreate app
  elif [[ -n "$OLD_CONTAINER_ID" ]]; then
    docker start "$OLD_CONTAINER_ID" >/dev/null
  fi

  compose ps >&2
  compose logs --tail=160 app >&2
  exit "$status"
}

test -d "$REMOTE_APP_DIR/.git" || { echo "Remote app directory is not a git repository: $REMOTE_APP_DIR" >&2; exit 1; }
command -v git >/dev/null 2>&1 || { echo "git is not installed on the server" >&2; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "docker is not installed on the server" >&2; exit 1; }
command -v curl >/dev/null 2>&1 || { echo "curl is not installed on the server" >&2; exit 1; }
command -v flock >/dev/null 2>&1 || { echo "flock is not installed on the server" >&2; exit 1; }

cd "$REMOTE_APP_DIR"
exec 9>.git/online-pos-deploy.lock
if ! flock -w 1200 9; then
  echo "Timed out waiting for another production deployment to finish" >&2
  exit 1
fi

git fetch origin "$DEPLOY_BRANCH"
git checkout "$DEPLOY_BRANCH"
git pull --ff-only origin "$DEPLOY_BRANCH"

ACTUAL_SHA="$(git rev-parse HEAD)"
if [[ "$ACTUAL_SHA" != "$DEPLOY_SHA" ]]; then
  echo "Refusing to deploy $ACTUAL_SHA; workflow tested $DEPLOY_SHA" >&2
  exit 1
fi

mkdir -p SupportConfigFiles
if [[ "$RUNTIME_ENV_CREATED" == "true" ]]; then
  test -f "$REMOTE_ENV" || { echo "Uploaded environment file missing: $REMOTE_ENV" >&2; exit 1; }
  mv "$REMOTE_ENV" SupportConfigFiles/.env
fi

test -f SupportConfigFiles/.env || { echo "SupportConfigFiles/.env is missing on the server" >&2; exit 1; }
chmod 600 SupportConfigFiles/.env
export GIT_SHA="$DEPLOY_SHA"

compose up -d db
OLD_CONTAINER_ID="$(compose ps -aq app 2>/dev/null || true)"
if [[ -n "$OLD_CONTAINER_ID" ]]; then
  if [[ "$(docker inspect --format '{{.State.Running}}' "$OLD_CONTAINER_ID")" != "true" ]]; then
    echo "Restarting the previous application container before deployment."
    docker start "$OLD_CONTAINER_ID" >/dev/null
  fi
  OLD_IMAGE_ID="$(docker inspect --format '{{.Image}}' "$OLD_CONTAINER_ID")"
  if docker image inspect "$OLD_IMAGE_ID" >/dev/null 2>&1; then
    docker tag "$OLD_IMAGE_ID" "$ROLLBACK_TAG"
  elif docker image inspect "$ROLLBACK_TAG" >/dev/null 2>&1; then
    echo "Using the existing rollback image because the running container image metadata is unavailable."
  else
    docker commit --pause=false "$OLD_CONTAINER_ID" "$ROLLBACK_TAG" >/dev/null
  fi
  ROLLBACK_AVAILABLE=true
fi

compose build app migrate

trap rollback ERR
# These commands must never inherit the SSH command stream as standard input.
compose run --rm --interactive=false provision </dev/null
compose run --rm --interactive=false migrate </dev/null
APP_RECREATED=true
compose up -d --no-deps --force-recreate app

APP_CONTAINER_ID="$(compose ps -q app)"
test -n "$APP_CONTAINER_ID"

for _ in $(seq 1 60); do
  HEALTH_STATUS="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "$APP_CONTAINER_ID")"
  if [[ "$HEALTH_STATUS" == "healthy" ]] && \
      curl --fail --silent --show-error --max-time 10 \
        --resolve "${PUBLIC_HOST}:443:127.0.0.1" "https://${PUBLIC_HOST}/" >/dev/null; then
    break
  fi
  sleep 2
done

HEALTH_STATUS="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "$APP_CONTAINER_ID")"
[[ "$HEALTH_STATUS" == "healthy" ]] || { echo "Application container is not healthy: $HEALTH_STATUS" >&2; false; }

IMAGE_REVISION="$(docker inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' "$APP_CONTAINER_ID")"
[[ "$IMAGE_REVISION" == "$DEPLOY_SHA" ]] || {
  echo "Running image revision $IMAGE_REVISION does not match tested revision $DEPLOY_SHA" >&2
  false
}

curl --fail --silent --show-error --max-time 10 \
  --resolve "${PUBLIC_HOST}:443:127.0.0.1" "https://${PUBLIC_HOST}/" >/dev/null

trap - ERR
if [[ "$ROLLBACK_AVAILABLE" == "true" ]]; then
  docker image rm "$ROLLBACK_TAG" >/dev/null 2>&1 || true
fi
compose ps
echo "Deployed and verified revision $DEPLOY_SHA"
