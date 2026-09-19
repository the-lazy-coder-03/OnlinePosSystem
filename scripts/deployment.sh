#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Universal deployment helper

Clone or update the application repository on a Linux server, upload the local
.env file to SupportConfigFiles/.env, and start the Docker Compose stack.

Required:
  --host <host>          Server IP address or hostname
  --user <user>          SSH username
  --key <path>           SSH private key path
  --repo <url>           Git repository URL
  --env-file <path>      Local .env file to upload

If a required option is missing and the script is running in a terminal, you
will be prompted to enter it.

Optional:
  --port <port>          SSH port (default: 22)
  --branch <branch>      Branch to checkout and pull
  --remote-base <path>   Remote base directory (default: ~/apps)
  --remote-dir <path>    Exact remote project directory
  --dry-run              Print actions without changing the server
  -h, --help             Show this help

Examples:
  scripts/deployment.sh

  scripts/deployment.sh \
    --host 130.131.162.110 \
    --user azureuser \
    --key ~/Downloads/crowdcam_key.pem \
    --repo https://github.com/the-lazy-coder-03/OnlinePosSystem.git \
    --env-file ./SupportConfigFiles/.env

  scripts/deployment.sh \
    --host 130.131.162.110 \
    --user azureuser \
    --key ~/Downloads/crowdcam_key.pem \
    --repo https://github.com/the-lazy-coder-03/OnlinePosSystem.git \
    --branch main \
    --env-file ./SupportConfigFiles/.env
EOF
}

error() {
  echo "Error: $*" >&2
  exit 1
}

require_value() {
  local option="$1"
  local value="${2:-}"

  if [[ -z "${value}" || "${value}" == --* ]]; then
    error "${option} requires a value"
  fi
}

require_command() {
  local command_name="$1"

  if ! command -v "${command_name}" >/dev/null 2>&1; then
    error "Required command '${command_name}' is not installed locally"
  fi
}

derive_repo_name() {
  local repo_url="$1"
  local trimmed="${repo_url%/}"
  local name="${trimmed##*/}"

  name="${name%.git}"

  if [[ -z "${name}" || "${name}" == "." || "${name}" == ".." ]]; then
    error "Could not derive a project folder name from repo URL: ${repo_url}"
  fi

  printf '%s\n' "${name}"
}

expand_local_path() {
  case "$1" in
    "~")
      printf '%s\n' "${HOME}"
      ;;
    "~/"*)
      printf '%s/%s\n' "${HOME}" "${1#~/}"
      ;;
    *)
      printf '%s\n' "$1"
      ;;
  esac
}

quote_shell() {
  printf '%q' "$1"
}

prompt_required() {
  local variable_name="$1"
  local current_value="$2"
  local prompt="$3"
  local value=""

  if [[ -n "${current_value}" ]]; then
    return
  fi

  if [[ ! -t 0 ]]; then
    error "${prompt} is required"
  fi

  while [[ -z "${value}" ]]; do
    read -r -p "${prompt}: " value
  done

  printf -v "${variable_name}" '%s' "${value}"
}

HOST=""
USER_NAME=""
KEY_PATH=""
REPO_URL=""
ENV_FILE=""
PORT="22"
BRANCH=""
REMOTE_BASE="~/apps"
REMOTE_DIR=""
DRY_RUN=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host)
      require_value "$1" "${2:-}"
      HOST="$2"
      shift 2
      ;;
    --user)
      require_value "$1" "${2:-}"
      USER_NAME="$2"
      shift 2
      ;;
    --key)
      require_value "$1" "${2:-}"
      KEY_PATH="$2"
      shift 2
      ;;
    --repo)
      require_value "$1" "${2:-}"
      REPO_URL="$2"
      shift 2
      ;;
    --env-file)
      require_value "$1" "${2:-}"
      ENV_FILE="$2"
      shift 2
      ;;
    --port)
      require_value "$1" "${2:-}"
      PORT="$2"
      shift 2
      ;;
    --branch)
      require_value "$1" "${2:-}"
      BRANCH="$2"
      shift 2
      ;;
    --remote-base)
      require_value "$1" "${2:-}"
      REMOTE_BASE="$2"
      shift 2
      ;;
    --remote-dir)
      require_value "$1" "${2:-}"
      REMOTE_DIR="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      error "Unknown option: $1"
      ;;
  esac
done

prompt_required HOST "${HOST}" "Server host or IP"
prompt_required USER_NAME "${USER_NAME}" "SSH username"
prompt_required KEY_PATH "${KEY_PATH}" "SSH private key path"
prompt_required REPO_URL "${REPO_URL}" "GitHub repository URL"
prompt_required ENV_FILE "${ENV_FILE}" "Local .env file path"

[[ -n "${HOST}" ]] || error "--host is required"
[[ -n "${USER_NAME}" ]] || error "--user is required"
[[ -n "${KEY_PATH}" ]] || error "--key is required"
[[ -n "${REPO_URL}" ]] || error "--repo is required"
[[ -n "${ENV_FILE}" ]] || error "--env-file is required"

require_command ssh
require_command scp
require_command git

KEY_PATH="$(expand_local_path "${KEY_PATH}")"
ENV_FILE="$(expand_local_path "${ENV_FILE}")"

[[ "${PORT}" =~ ^[0-9]+$ ]] || error "--port must be a number"
[[ -f "${KEY_PATH}" ]] || error "SSH key not found: ${KEY_PATH}"
[[ -f "${ENV_FILE}" ]] || error ".env file not found: ${ENV_FILE}"

PROJECT_NAME="$(derive_repo_name "${REPO_URL}")"
if [[ -z "${REMOTE_DIR}" ]]; then
  REMOTE_DIR="${REMOTE_BASE%/}/${PROJECT_NAME}"
fi

SSH_TARGET="${USER_NAME}@${HOST}"
SSH_ARGS=(-i "${KEY_PATH}" -p "${PORT}" "${SSH_TARGET}")
SCP_ARGS=(-i "${KEY_PATH}" -P "${PORT}")

echo "Preparing deployment"
echo "  Host: ${SSH_TARGET}"
echo "  Repository: ${REPO_URL}"
echo "  Remote directory: ${REMOTE_DIR}"
echo "  Env destination: ${REMOTE_DIR}/SupportConfigFiles/.env"
if [[ -n "${BRANCH}" ]]; then
  echo "  Branch: ${BRANCH}"
fi

if [[ "${DRY_RUN}" == true ]]; then
  echo
  echo "Dry run: no server changes will be made."
  echo "Would run: chmod 600 $(quote_shell "${KEY_PATH}")"
  echo "Would SSH to: ${SSH_TARGET} on port ${PORT}"
  echo "Would clone or update repo at: ${REMOTE_DIR}"
  echo "Would upload: ${ENV_FILE} -> ${REMOTE_DIR}/SupportConfigFiles/.env"
  echo "Would run: scripts/init-letsencrypt.sh"
  exit 0
fi

chmod 600 "${KEY_PATH}"

remote_script='
set -euo pipefail

expand_remote_path() {
  case "$1" in
    "~")
      printf "%s\n" "$HOME"
      ;;
    "~/"*)
      printf "%s/%s\n" "$HOME" "${1#~/}"
      ;;
    *)
      printf "%s\n" "$1"
      ;;
  esac
}

repo_url="$1"
remote_dir_input="$2"
branch="$3"

remote_dir="$(expand_remote_path "${remote_dir_input}")"
remote_parent="$(dirname "${remote_dir}")"

if ! command -v git >/dev/null 2>&1; then
  echo "Error: git is not installed on the server" >&2
  exit 1
fi

mkdir -p "${remote_parent}"

if [[ ! -e "${remote_dir}" ]]; then
  git clone "${repo_url}" "${remote_dir}"
elif [[ -d "${remote_dir}/.git" ]]; then
  git -C "${remote_dir}" fetch --prune
else
  echo "Error: ${remote_dir} exists but is not a git repository" >&2
  exit 1
fi

if [[ -n "${branch}" ]]; then
  git -C "${remote_dir}" checkout "${branch}"
fi

git -C "${remote_dir}" pull --ff-only

printf "%s\n" "${remote_dir}"
'

echo
echo "Cloning or updating repository on server..."
REMOTE_DIR_EXPANDED="$(ssh "${SSH_ARGS[@]}" "bash -s" -- "${REPO_URL}" "${REMOTE_DIR}" "${BRANCH}" <<< "${remote_script}" | tail -n 1)"

echo "Uploading .env file..."
ssh "${SSH_ARGS[@]}" "mkdir -p '${REMOTE_DIR_EXPANDED}/SupportConfigFiles'"
scp "${SCP_ARGS[@]}" "${ENV_FILE}" "${SSH_TARGET}:${REMOTE_DIR_EXPANDED}/SupportConfigFiles/.env"

echo "Starting Docker Compose and HTTPS setup..."
ssh "${SSH_ARGS[@]}" \
  "cd '${REMOTE_DIR_EXPANDED}' && chmod +x scripts/init-letsencrypt.sh && scripts/init-letsencrypt.sh"

echo
echo "Deployment files are ready."
echo "Repository: ${SSH_TARGET}:${REMOTE_DIR_EXPANDED}"
echo "Env file: ${SSH_TARGET}:${REMOTE_DIR_EXPANDED}/SupportConfigFiles/.env"
