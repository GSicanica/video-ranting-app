#!/usr/bin/env bash
set -euo pipefail

# Generate and optionally execute curl commands for production APIs
# Usage: ./scripts/curl_all_production.sh [--execute]

BASE_API="https://tmbv-hms.com/ayoutube/api"
ADMIN_API="https://tmbv-hms.com/aaayoutube/api"
COOKIE_JAR="./tmp/curl_cookies.txt"
EXECUTE=false

if [[ ${1:-} == "--execute" ]]; then
  EXECUTE=true
fi

mkdir -p "$(dirname "$COOKIE_JAR")"
> "$COOKIE_JAR"

# Helper to run or print a curl command
run_or_print() {
  local cmd="$1"
  if [ "$EXECUTE" = true ]; then
    echo "--> Executing: $cmd"
    # shellcheck disable=SC2086
    eval $cmd
    echo
  else
    echo "# $cmd"
  fi
}

# Admin login example (can be provided via env ADMIN_USER and ADMIN_PASS)
if [[ -n "${ADMIN_USER:-}" && -n "${ADMIN_PASS:-}" ]]; then
  ADMIN_LOGIN_CMD="curl -sS -X POST -H 'Content-Type: application/json' -c $COOKIE_JAR -d '{\"username\":\"'$ADMIN_USER'\",\"password\":\"'$ADMIN_PASS'\"}' $ADMIN_API/admin/admin-login.php"
  run_or_print "$ADMIN_LOGIN_CMD"
else
  run_or_print "# ADMIN_USER/ADMIN_PASS not set. To login run: curl -sS -X POST -H 'Content-Type: application/json' -c $COOKIE_JAR -d '{\"username\":\"ADMIN_USER\",\"password\":\"ADMIN_PASS\"}' $ADMIN_API/admin/admin-login.php"
fi

# Discover API PHP files and emit curl commands
find_backend_php_files() {
  find backend-php/api -type f -name '*.php' | sort
}
find_aaayoutube_api_files() {
  find aaayoutube/api -type f -name '*.php' | sort
}

emit_for_file() {
  local file="$1"
  local rel="${file#*/}"    # strip leading directory like backend-php/
  local url=""
  local use_admin_cookie=false

  if [[ "$file" == aaayoutube/api/* ]]; then
    url="$ADMIN_API/${file#aaayoutube/api/}"
    use_admin_cookie=true
  else
    url="$BASE_API/${file#backend-php/api/}"
  fi

  # Guess method by checking for REQUEST_METHOD or POST usages in file
  if grep -q "REQUEST_METHOD" "$file" || grep -q "\$_POST" "$file" || grep -q "file_get_contents('php://input'" "$file" || grep -q "\$\_SERVER\['REQUEST_METHOD'\]" "$file"; then
    method="POST"
  else
    method="GET"
  fi

  # Provide a small set of useful param examples for known endpoints
  case "$rel" in
    "analytics/get-analytics.php")
      params='?days=30'
      method=GET
      ;;
    "users/export-data.php")
      params='?device_id=test-device&format=json'
      method=GET
      ;;
    "ratings/get-by-device.php")
      params='?device_id=test-device'
      method=GET
      ;;
    "videos/recommendations.php")
      params='?video_id=dQw4w9WgXcQ&limit=10'
      method=GET
      ;;
    "comments/add.php")
      method=POST
      data='{"rating_id":123,"comment_text":"Nice video!"}'
      ;;
    "favorites/sync.php")
      method=POST
      data='{"device_id":"test-device","video_ids":["id1","id2"]}'
      ;;
    *)
      params=''
      ;;
  esac

  if [ "$method" = "GET" ]; then
    cmd="curl -sS -X GET \"$url$params\""
    if $use_admin_cookie; then
      cmd+=" -b $COOKIE_JAR"
    fi
    run_or_print "$cmd"
  else
    # POST
    : "${data:='{"sample":"value"}'}"
    cmd="curl -sS -X POST -H 'Content-Type: application/json' -d '$data' \"$url\""
    if $use_admin_cookie; then
      cmd+=" -b $COOKIE_JAR -c $COOKIE_JAR"
    fi
    run_or_print "$cmd"
  fi
}

# Emit for backend-php APIs
for f in $(find_backend_php_files); do
  emit_for_file "$f"
done

# Emit for aaayoutube APIs
for f in $(find_aaayoutube_api_files); do
  emit_for_file "$f"
done

echo "\nDone. Run with --execute to actually make the requests and save admin cookie (if any)."
