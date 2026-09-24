#!/usr/bin/env bash
set -euo pipefail
: "${MIGRATION_DATASOURCE_URL:?Set the owner JDBC URL}"
: "${MIGRATION_DATASOURCE_USERNAME:?Set the owner username}"
: "${MIGRATION_DATASOURCE_PASSWORD:?Set the owner password}"
jar_path="${1:?Usage: scripts/migrate-database.sh /path/to/app.jar}"
# Environment variables override profile properties. Ensure exported runtime
# credentials cannot replace the owner credentials in application-migrate.properties.
export SPRING_DATASOURCE_URL="$MIGRATION_DATASOURCE_URL"
export SPRING_DATASOURCE_USERNAME="$MIGRATION_DATASOURCE_USERNAME"
export SPRING_DATASOURCE_PASSWORD="$MIGRATION_DATASOURCE_PASSWORD"
java -jar "$jar_path" --spring.profiles.active=migrate
