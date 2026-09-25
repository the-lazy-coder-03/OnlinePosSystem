#!/usr/bin/env bash
set -euo pipefail
: "${PGDATABASE:?Set PGDATABASE to the application database}"
: "${MIGRATION_DATASOURCE_USERNAME:?Set the migration owner username}"
: "${MIGRATION_DATASOURCE_PASSWORD:?Set the migration owner password}"
: "${SPRING_DATASOURCE_USERNAME:?Set the restricted runtime username}"
: "${SPRING_DATASOURCE_PASSWORD:?Set the runtime password}"
if [[ "$MIGRATION_DATASOURCE_USERNAME" == "$SPRING_DATASOURCE_USERNAME" ]]; then
  echo 'Owner and runtime accounts must be different' >&2
  exit 1
fi
# PGHOST/PGPORT/PGUSER/PGPASSWORD identify the administrator connection.
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
sql_file="${script_dir}/../SQL files/provision-rls.sql"
test -f "$sql_file" || { echo "Missing SQL file: $sql_file" >&2; exit 1; }
psql -X --dbname="$PGDATABASE" --file="$sql_file"
