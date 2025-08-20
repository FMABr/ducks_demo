#!/bin/bash
TITLE=$1
if [ -z "$TITLE" ]; then
  echo "Usage: $0 <title>"
  exit 1
fi

MIGRATION_DIR="src/main/resources/db/migration"

# Find the highest existing version number (strip leading V and take the number before __)
LAST_VERSION=$(ls "$MIGRATION_DIR"/V[0-9]*__*.sql 2>/dev/null \
  | sed -E 's/.*V([0-9]+)__.*\.sql/\1/' \
  | sort -n \
  | tail -1)

if [ -z "$LAST_VERSION" ]; then
  NEXT_VERSION=1
else
  NEXT_VERSION=$((LAST_VERSION + 1))
fi

TIMESTAMP=$(date +%Y%m%d%H%M%S)
FILENAME="$MIGRATION_DIR/V${NEXT_VERSION}__${TITLE}_${TIMESTAMP}.sql"

touch "$FILENAME"
echo "Created $FILENAME"
