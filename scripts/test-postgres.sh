#!/usr/bin/env bash
set -euo pipefail

: "${RLS_TEST_ADMIN_URL:?Set a PostgreSQL JDBC URL for a disposable test server}"
: "${RLS_TEST_ADMIN_USERNAME:?Set a PostgreSQL administrator username}"
if [[ "$RLS_TEST_ADMIN_URL" != jdbc:postgresql://*/* ]]; then
  echo 'RLS_TEST_ADMIN_URL must be a PostgreSQL JDBC URL ending in a database name' >&2
  exit 1
fi

admin_uri="${RLS_TEST_ADMIN_URL#jdbc:}"
test_db="pos_test_$(date +%s)_$$"
export PGUSER="$RLS_TEST_ADMIN_USERNAME"
export PGPASSWORD="${RLS_TEST_ADMIN_PASSWORD:-}"
export TEST_DATASOURCE_URL="${RLS_TEST_ADMIN_URL%/*}/$test_db"
export TEST_DATASOURCE_USERNAME="$RLS_TEST_ADMIN_USERNAME"
export TEST_DATASOURCE_PASSWORD="${RLS_TEST_ADMIN_PASSWORD:-}"
export SPRING_PROFILES_ACTIVE=postgres-test

cleanup() {
  psql -X -q -d "$admin_uri" -c "DROP DATABASE IF EXISTS $test_db WITH (FORCE)" >/dev/null 2>&1 || true
}
trap cleanup EXIT
psql -X -v ON_ERROR_STOP=1 -q -d "$admin_uri" -c "CREATE DATABASE $test_db"

if (( $# == 0 )); then
  set -- verify -Prls-it
fi
"$(dirname "$0")/../Misc/mvnw" -f "$(dirname "$0")/../Misc/pom.xml" "$@"
