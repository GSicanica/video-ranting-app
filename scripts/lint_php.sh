#!/usr/bin/env bash
# Lint all PHP files in the repo (shallow search)
set -e

echo "Running php -l on PHP files..."
FAIL=0
while IFS= read -r -d '' file; do
  echo "Checking: $file"
  if ! php -l "$file" >/dev/null 2>&1; then
    echo "  -> SYNTAX ERROR in $file"
    php -l "$file"
    FAIL=1
  fi
done < <(find . -name '*.php' -type f -not -path './vendor/*' -print0)

if [ "$FAIL" -ne 0 ]; then
  echo "One or more PHP files failed syntax check."
  exit 2
fi

echo "All PHP files passed syntax check."