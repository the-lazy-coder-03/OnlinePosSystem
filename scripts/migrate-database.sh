#!/usr/bin/env bash
set -euo pipefail
: "${MIGRATION_DATASOURCE_URL:?Set the owner JDBC URL}"
: "${MIGRATION_DATASOURCE_USERNAME:?Set the owner username}"
: "${MIGRATION_DATASOURCE_PASSWORD:?Set the owner password}"
jar_path="${1:?Usage: scripts/migrate-database.sh /path/to/app.jar}"
java -jar "$jar_path" --spring.profiles.active=migrate
