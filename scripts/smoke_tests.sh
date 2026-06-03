#!/usr/bin/env bash
# Simple smoke test script for key endpoints
# Usage: ./scripts/smoke_tests.sh https://example.com

BASE_URL=${1:-http://localhost}
ENDPOINTS=(
  "/api/analytics/get-analytics.php"
  "/api/social/activity-feed.php"
  "/api/ratings/get-by-device.php?device_id=test-device"
  "/api/discovery/trending.php"
  "/api/videos/recommendations.php"
  "/api/admin/admin-export.php?format=json" # requires admin auth on some installs; may return 401
  "/api/admin/admin-list-videos.php"
)

echo "Running smoke tests against: $BASE_URL"

for ep in "${ENDPOINTS[@]}"; do
  url="$BASE_URL$ep"
  echo "\nTesting: $url"
  http_code=$(curl -s -w "%{http_code}" -o /tmp/smoke_output.txt "$url")
  echo "HTTP: $http_code"
  head -c 100 /tmp/smoke_output.txt | sed -n '1,5p'
  if [[ "$http_code" -ge 500 ]]; then
    echo "  => SERVER ERROR"
  elif [[ "$http_code" -eq 401 ]]; then
    echo "  => UNAUTHORIZED (expected for admin endpoints without credentials)"
  elif [[ "$http_code" -eq 200 ]]; then
    # quick JSON success check
    if grep -q '"success"' /tmp/smoke_output.txt; then
      echo "  => OK (contains success field)"
    else
      echo "  => OK (no success field)"
    fi
  else
    echo "  => HTTP $http_code"
  fi
done

rm -f /tmp/smoke_output.txt

echo "Smoke tests complete."