#!/bin/bash
set -euo pipefail

# iOS Build Script for YouTube Rating App
# This script is called by Xcode's Build Phases to compile Kotlin Multiplatform frameworks

REPO_ROOT="$SRCROOT/.."
GRADLE_WRAPPER="$REPO_ROOT/gradlew"

echo "📦 Building YouTube Rating App Kotlin Frameworks for iOS"
echo "Repository root: $REPO_ROOT"
echo "Configuration: $CONFIGURATION"
echo "Platform: $PLATFORM_NAME"

cd "$REPO_ROOT"
echo "Running Gradle build..."
"$GRADLE_WRAPPER" iosAppAssembleXCFrameworks \
    --no-daemon \
    --console=plain \
    -x test

# Sync frameworks to Xcode project
echo "📤 Syncing frameworks to Xcode project..."
"$GRADLE_WRAPPER" iosAppSyncXCFrameworks \
    --no-daemon \
    --console=plain

echo "✅ Kotlin frameworks built successfully!"
