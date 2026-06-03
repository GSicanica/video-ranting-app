#!/usr/bin/env bash
# Trigger multiple reports against a video to test auto-delete behavior.
# WARNING: This will create data and may delete videos if threshold met. Use only against test environments.
# Usage: ./scripts/trigger_reports.sh <base_url> <video_id> <count> [--device-prefix=dev] [--yes]

BASE_URL="$1"
VIDEO_ID="$2"
COUNT="$3"
DEVICE_PREFIX="dev"
CONFIRM=0

shift 3 || true
for arg in "$@"; do
  case $arg in
    --device-prefix=*) DEVICE_PREFIX="${arg#*=}" ;;
    --yes) CONFIRM=1 ;;
  esac
done

if [ -z "$BASE_URL" ] || [ -z "$VIDEO_ID" ] || [ -z "$COUNT" ]; then
  echo "Usage: $0 <base_url> <video_id> <count> [--device-prefix=dev] [--yes]"
  exit 2
fi

if [ "$CONFIRM" -ne 1 ]; then
  echo "This will POST $COUNT reports to $VIDEO_ID at $BASE_URL/api/reports/report-video.php"
  echo "Run again with --yes to execute."
  exit 0
fi

for i in $(seq 1 $COUNT); do
  DEVICE_ID="$DEVICE_PREFIX-$i-$(date +%s%N)"
  echo "Posting report $i/$COUNT with device $DEVICE_ID"
  curl -s -X POST "$BASE_URL/api/reports/report-video.php" -H "Content-Type: application/json" -d "{\"videoId\": \"$VIDEO_ID\", \"deviceId\": \"$DEVICE_ID\", \"reason\": \"test-auto-delete\"}" | sed -n '1,3p'
  sleep 0.3
done

echo "Done."