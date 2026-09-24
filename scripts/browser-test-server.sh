#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${RLS_TEST_ADMIN_URL:?Set an administrator JDBC URL for a disposable PostgreSQL server}"
: "${RLS_TEST_ADMIN_USERNAME:?Set a test PostgreSQL administrator}"
[[ "$RLS_TEST_ADMIN_URL" == jdbc:postgresql://*/* ]] || exit 1

suffix="$(date +%s)_$$"
test_db="pos_browser_${suffix}"
owner="browser_owner_${suffix}"
runtime="browser_runtime_${suffix}"
admin_uri="${RLS_TEST_ADMIN_URL#jdbc:}"
test_uri="${admin_uri%/*}/${test_db}"
export PGUSER="$RLS_TEST_ADMIN_USERNAME" PGPASSWORD="${RLS_TEST_ADMIN_PASSWORD:-}"
export MIGRATION_DATASOURCE_USERNAME="$owner" MIGRATION_DATASOURCE_PASSWORD=browser-test-only
export SPRING_DATASOURCE_USERNAME="$runtime" SPRING_DATASOURCE_PASSWORD=browser-test-only
export MIGRATION_DATASOURCE_URL="jdbc:${test_uri}" RLS_RUNTIME_ROLE="$runtime"
app_pid=''
cleanup() {
    if [[ -n "$app_pid" ]]; then kill "$app_pid" 2>/dev/null || true; wait "$app_pid" 2>/dev/null || true; fi
    psql -X -q -d "$admin_uri" -c "DROP DATABASE IF EXISTS $test_db WITH (FORCE)" >/dev/null
    psql -X -q -d "$admin_uri" -c "DROP ROLE IF EXISTS $runtime; DROP ROLE IF EXISTS $owner" >/dev/null
}
trap cleanup EXIT
trap 'exit 0' TERM INT
psql -X -v ON_ERROR_STOP=1 -q -d "$admin_uri" -c "CREATE DATABASE $test_db"
PGDATABASE="$test_uri" bash scripts/provision-rls.sh

jar=target/OnlinePosSystem-0.0.1-SNAPSHOT.jar
java -jar "$jar" --spring.config.import= --spring.profiles.active=migrate \
    --spring.datasource.username="$owner" --spring.datasource.password=browser-test-only \
    --spring.datasource.url="jdbc:${test_uri}" --app.database.migration.enabled=true

psql -X -v ON_ERROR_STOP=1 -q -d "$test_uri" <<'SQL'
INSERT INTO customers(email,password,access_level,role,first_name,last_name)
VALUES ('browser@example.com','{noop}Browser-test-123',0,'USER','Browser','Customer'),
       ('branch@example.com','{noop}Browser-test-123',1,'ADMIN','Branch','Admin');
SQL

java -jar "$jar" --spring.config.import= --spring.profiles.active=default \
    --spring.datasource.url="jdbc:${test_uri}" --spring.datasource.username="$runtime" \
    --spring.datasource.password=browser-test-only --server.port=18081 --server.address=127.0.0.1 \
    --ADMIN_USERNAME=browser-admin --ADMIN_PASSWORD=Browser-test-123 \
    --jwt.secret=browser-tests-only-secret-at-least-32-bytes \
    --SESSION_COOKIE_SECURE=false --APP_BASE_URL=http://127.0.0.1:18081 \
    --RESEND_API_KEY= --MAIL_API= --GOOGLE_MAPS_API_KEY= \
    --logging.level.org.springframework.security=INFO &
app_pid=$!
wait "$app_pid"
