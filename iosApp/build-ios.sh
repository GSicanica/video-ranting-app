#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
./gradlew iosAppSyncXCFrameworks

echo "Updated iosApp/shared.xcframework and iosApp/composeApp.xcframework"
