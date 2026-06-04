# Gradle Documentation

This document explains how Gradle is used in this repository. It is specific to
the current Android/Kotlin Multiplatform migration shape of YouTube Rating App.

## Quick Commands

Use the wrapper from the repository root:

```bash
./gradlew productionArchitectureCheck
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:lintRelease
./gradlew :androidApp:bundleRelease
```

For local build performance measurements:

```bash
gradle-profiler --benchmark \
  --project-dir . \
  --scenario-file gradle-profiler.scenarios \
  --output-dir profile-out
```

Generated profiler output is ignored by Git:

```text
gradle-user-home/
profile-out/
```

## Project Layout

The root project is `YouTubeRatingApp`. Module inclusion is controlled from
`settings.gradle.kts`.

Main modules:

| Module | Purpose |
|---|---|
| `:androidApp` | Thin Android application launcher, manifest, signing, packaging, variants |
| `:composeApp` | Main Android/KMP app shell and most current Android UI/runtime code |
| `:shared` | Kotlin Multiplatform shared code and Realm/Ktor shared infrastructure |
| `:core:core-domain` | Shared domain contracts and pure domain code |
| `:core:core-data` | Shared Android data, storage, network, workers, managers |
| `:core:core-presentation` | Shared presentation/MVI helpers |
| `:core:core-design-system` | Shared Compose design system |
| `:feature:*-(domain,data,presentation)` | Feature extraction target modules |
| `:baselineprofile` | Android baseline profile and startup benchmark module |
| `:desktopApp` | Scaffold only, no runtime target yet |
| `:webApp` | Scaffold only, no runtime target yet |
| `:iosComposeApp` | Optional iOS Compose/KMP framework module |

`iosComposeApp` is included only when this property is enabled:

```bash
./gradlew -PenableIosBuild=true iosAppAssembleXCFrameworks
```

## Repository Resolution

`settings.gradle.kts` defines plugin and dependency repositories.

Plugin repositories:

```text
google()
gradlePluginPortal()
mavenCentral()
maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
```

Dependency repositories:

```text
google()
mavenCentral()
maven("https://jitpack.io")
maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
```

The build uses:

```kotlin
repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
includeBuild("build-logic")
```

That means new repositories should normally be added in `settings.gradle.kts`,
not inside individual modules.

## Version Catalog

All shared dependency and plugin versions live in:

```text
gradle/libs.versions.toml
```

Use catalog aliases instead of hardcoded versions in build files.

Examples:

```kotlin
implementation(libs.coroutines.android)
implementation(libs.bundles.compose.screen)
alias(libs.plugins.kotlin.serialization)
```

Central Android SDK and app version values live in `gradle.properties`:

```properties
android.compileSdk=36
android.minSdk=24
android.targetSdk=35
app.versionCode=70
app.versionName=7.0
```

`androidApp` and `composeApp` both read `app.versionCode` and
`app.versionName` so BuildConfig and packaging stay aligned.

## Build Logic

Reusable Gradle conventions are implemented in the included build:

```text
build-logic/
```

Registered plugin IDs:

| Plugin ID | Implementation | Use |
|---|---|---|
| `youtube.android.application` | `AndroidApplicationConventionPlugin` | Android app modules |
| `youtube.android.library` | `AndroidLibraryConventionPlugin` | Android library modules |
| `youtube.android.compose` | `AndroidComposeConventionPlugin` | Compose setup for Android app/library modules |
| `youtube.android.feature` | `AndroidFeatureConventionPlugin` | Feature presentation modules |
| `youtube.android.feature.coil` | `AndroidFeatureCoilConventionPlugin` | Feature modules needing Coil on top of feature defaults |
| `youtube.kmp.library` | `KmpLibraryConventionPlugin` | KMP Android library modules |

### `youtube.android.application`

Applies:

```text
com.android.application
org.jetbrains.kotlin.android
```

Defaults:

```text
compileSdk from android.compileSdk
minSdk from android.minSdk
targetSdk from android.targetSdk
Java 17
Kotlin JVM target 17
BuildConfig enabled
coreLibraryDesugaring enabled
```

### `youtube.android.library`

Applies:

```text
com.android.library
org.jetbrains.kotlin.android
```

Defaults:

```text
compileSdk from android.compileSdk
minSdk from android.minSdk
Java 17
Kotlin JVM target 17
-Xskip-metadata-version-check
BuildConfig enabled
coreLibraryDesugaring enabled
```

Feature convenience wiring:

```text
:feature:*-domain -> implementation(:core:core-domain)
:feature:*-data   -> implementation(:core:core-data) and own domain module when present
```

The plugin also patches selected Jar tasks to include Kotlin class output from
`tmp/kotlin-classes`.

### `youtube.android.compose`

Applies:

```text
org.jetbrains.kotlin.plugin.compose
org.jetbrains.compose
```

Adds:

```text
compose BOM
compose-base bundle
compose-debug bundle in debugImplementation
```

Compiler opt-ins:

```text
androidx.compose.material3.ExperimentalMaterial3Api
androidx.compose.foundation.ExperimentalFoundationApi
```

### `youtube.android.feature`

Builds on:

```text
youtube.android.library
youtube.android.compose
```

Adds:

```text
:core:core-presentation
:core:core-design-system
own :feature:<name>-domain when present
compose-screen bundle
koin-compose bundle
coil-compose
compose-markdown
paging runtime/ktx/compose
serialization-json
sentry-android
youtube-player
media3 bundle
```

Current migration note: many presentation modules still manually depend on
`:core:core-data`. Prefer moving those dependencies behind domain contracts when
touching those areas.

### `youtube.kmp.library`

Applies:

```text
org.jetbrains.kotlin.multiplatform
com.android.library
```

Defaults:

```text
androidTarget()
compileSdk from android.compileSdk
minSdk from android.minSdk
Java 17
Kotlin JVM target 17
BuildConfig disabled
coreLibraryDesugaring enabled
```

`composeApp` enables BuildConfig again because existing Android runtime code
uses `BuildConfig`.

## `buildSrc`

`buildSrc` contains project-specific helper code used by the root build.

Important files:

| File | Purpose |
|---|---|
| `ProductionArchitectureCheckTask.kt` | Implements `productionArchitectureCheck` |
| `ComposeMetrics.kt` | Helper functions for Compose compiler metrics/reports flags |

`productionArchitectureCheck` validates production guardrails and reports
migration debt.

It fails on:

```text
missing required architecture paths
restored/used :core:legacy module
non-launcher Kotlin/Java sources in androidApp/src/main
domain importing data/network directly
empty feature data/domain modules
feature presentation modules depending on other feature presentation modules
```

It reports, but does not fail, migration signals such as:

```text
Dispatchers.IO usage
KoinJavaComponent.get usage
getInstance usage
presentation -> feature data build dependencies
core-data UI dependency declarations
```

Run it before and after architecture-sensitive Gradle/module changes:

```bash
./gradlew productionArchitectureCheck
```

## Root Build

`build.gradle.kts` applies plugin aliases with `apply false` and configures all
projects.

Kotlin alignment:

```text
All non-kapt configurations align kotlin-stdlib* and kotlin-reflect to the
catalog Kotlin version.
```

This avoids KMP metadata/native sensitivity while avoiding over-pinning Android
runtime classpaths.

Root custom tasks:

| Task | Purpose |
|---|---|
| `productionArchitectureCheck` | Validates architecture guardrails |
| `iosAppAssembleXCFrameworks` | Builds shared and composeApp XCFrameworks when `enableIosBuild=true` |
| `iosAppSyncXCFrameworks` | Syncs those XCFrameworks into `iosApp/` when `enableIosBuild=true` |

## `androidApp`

`androidApp` is the installable Android application module. It should stay thin.

Key responsibilities:

```text
applicationId = com.youtube.rating.android
manifest and placeholders
debug/release build types
signing config
packaging rules
release R8/shrinking
launcher-only source set
depends on :composeApp
```

Source set rule:

```kotlin
java.setSrcDirs(listOf("src/launcher/kotlin"))
```

Do not add application logic to `androidApp/src/main`. Put logic in
`composeApp`, `core:*`, or `feature:*`.

Debug build:

```text
applicationIdSuffix = .debug
versionNameSuffix = -debug
minify disabled
shrinkResources disabled
cleartext traffic allowed
Sentry traces/profiling sample rate 0.0 by default
```

Release build:

```text
minify enabled
shrinkResources enabled
optimized default ProGuard file
androidApp/proguard-rules.pro
HTTPS/WSS only by default
ABI split enabled for AAB
debug signing fallback when key.properties is missing
```

Local release signing file:

```text
androidApp/src/key.properties
```

Expected keys:

```properties
keyAlias=...
keyPassword=...
storeFile=...
storePassword=...
```

This file is ignored by Git.

## `composeApp`

`composeApp` is currently the main Android/KMP migration host.

It uses:

```text
youtube.kmp.library
org.jetbrains.compose
org.jetbrains.kotlin.plugin.compose
org.jetbrains.kotlin.plugin.serialization
```

Android namespace:

```text
com.youtube.rating.android
```

This is intentionally kept so existing packages continue resolving `R` and
`BuildConfig` without mass refactors.

Important BuildConfig fields:

```text
VERSION_NAME
VERSION_CODE
APPLICATION_ID
MIN_SDK
TARGET_SDK
COMPILE_SDK
ENABLE_LOGGING
DEBUG_UNLOCK_PASSWORD
BASE_URL
USE_LOCAL_SERVER
```

`BASE_URL` comes from:

```text
local.properties BASE_URL
or -Papp.baseUrl=...
or debug default https://tmbv-hms.com/backend-fastapi
or release default https://tmbv-hms.com/backend-fastapi
```

Bible asset sync:

```text
source: backend-php/output1/**/*.md
target: composeApp/build/generated/bibleAssets/output1
task: syncBibleAssets
```

The sync task is a no-op when `backend-php/output1` does not exist.

KMP source sets:

| Source set | Notes |
|---|---|
| `commonMain` | Shared Compose/runtime/Koin/settings dependencies |
| `androidMain` | Main Android implementation and feature/core dependencies |
| `androidUnitTest` | Adds running feature dependencies for current tests |

`androidMain` excludes:

```text
**/feature/running/**
```

The running feature is compiled from extracted feature modules instead.

## `shared`

`shared` is a KMP library using:

```text
youtube.kmp.library
kotlin serialization
Realm Kotlin
```

Targets:

```text
androidTarget always
iosArm64 and iosSimulatorArm64 only when -PenableIosBuild=true
```

Source set shape:

```text
commonMain
realmMain
androidMain
iosMain when enabled
```

`realmMain` depends on `commonMain` and exports Realm base APIs with `api(...)`.

XCFramework sync:

```bash
./gradlew -PenableIosBuild=true :shared:syncSharedXCFrameworkToXcode
```

## `iosComposeApp`

`iosComposeApp` is included only with:

```bash
-PenableIosBuild=true
```

It builds the iOS Compose framework named:

```text
composeApp.xcframework
```

It exports `:shared` from its framework.

Root convenience tasks:

```bash
./gradlew -PenableIosBuild=true iosAppAssembleXCFrameworks
./gradlew -PenableIosBuild=true iosAppSyncXCFrameworks
```

## Feature Modules

Most features follow the same physical split:

```text
feature/<name>-domain
feature/<name>-data
feature/<name>-presentation
```

General rules:

```text
domain should be pure and depend only on core-domain/shared-safe APIs
data implements domain contracts and may use core-data
presentation uses own domain, core-presentation, core-design-system, Compose
features should not directly depend on other feature implementations
```

Current migration reality:

```text
Many Android packages are still com.youtube.rating.android.*
Many presentation modules still manually depend on :core:core-data
composeApp still depends on many feature data and presentation modules
```

When adding a new feature module, prefer:

```kotlin
plugins {
    id("youtube.android.library") // domain/data
}
```

or:

```kotlin
plugins {
    id("youtube.android.feature") // presentation
}
```

Add dependencies through `gradle/libs.versions.toml`.

## Core Modules

| Module | Plugin | Notes |
|---|---|---|
| `core-domain` | `youtube.android.library` | Domain contracts/models |
| `core-data` | `youtube.android.library` + serialization | Shared Android data/network/storage |
| `core-presentation` | `youtube.android.library` + `youtube.android.compose` + serialization | MVI and presentation helpers |
| `core-design-system` | `youtube.android.library` + `youtube.android.compose` | Theme/components/design system |

`core-data` currently carries a lot of migration-era Android infrastructure.
Avoid moving UI dependencies into it.

## Desktop and Web

`desktopApp` and `webApp` are Gradle scaffold modules only.

Informational tasks:

```bash
./gradlew :desktopApp:desktopAppInfo
./gradlew :webApp:webAppInfo
```

They do not currently build runtime desktop/web apps.

## Baseline Profile and Benchmarks

`baselineprofile` is an Android test module targeting:

```text
:androidApp
```

Main task alias:

```bash
./gradlew :baselineprofile:connectedBenchmark
```

Underlying task:

```text
:baselineprofile:connectedBenchmarkBenchmarkAndroidTest
```

Defaults:

```text
targetAppId = com.youtube.rating.android
androidx.benchmark.enabledRules = baselineprofile
androidx.benchmark.skipOnEmulator = true
```

Startup macrobenchmarks are gated in test code and can be enabled with:

```bash
./gradlew :baselineprofile:connectedBenchmark -PrunStartupBenchmarks=true
```

Use a physical device for meaningful macrobenchmark/profile generation.

## Build Properties

Important `gradle.properties` settings:

| Property | Current value | Meaning |
|---|---:|---|
| `org.gradle.jvmargs` | `-Xmx6144m ...` | Main Gradle JVM memory/GC tuning |
| `org.gradle.caching` | `false` | Local build cache disabled due Android DEX cache instability |
| `org.gradle.parallel` | `false` | Parallel execution disabled because KMP + KAPT setup is unstable |
| `org.gradle.configureondemand` | `true` | Configure only needed project subset where possible |
| `org.gradle.configuration-cache` | `true` | Configuration cache enabled |
| `org.gradle.configuration-cache.problems` | `warn` | Warn rather than fail on configuration-cache problems |
| `kotlin.incremental` | `true` | Kotlin incremental compilation enabled |
| `kotlin.caching.enabled` | `true` | Kotlin caches enabled |
| `kotlin.compiler.execution.strategy` | `out-of-process` | Kotlin compiler runs out of Gradle process |
| `org.gradle.vfs.watch` | `true` | File system watching enabled |
| `android.enableJetifier` | `false` | Jetifier disabled |
| `android.enableR8.fullMode` | `true` | R8 full mode enabled |
| `android.nonTransitiveRClass` | `true` | Non-transitive R classes enabled |

Do not flip `org.gradle.caching` or `org.gradle.parallel` casually. Both are
intentionally disabled for project-specific stability reasons.

## Local Properties and Secrets

Local machine values are read from:

```text
local.properties
androidApp/src/key.properties
```

Common `local.properties` keys:

```properties
BASE_URL=...
GOOGLE_MAPS_API_KEY=...
```

`YOUVERSION_API_KEY` is a backend secret. Configure it in the FastAPI service
environment, not in Android `local.properties`.

Do not commit Sentry DSNs or provider secrets. Android release builds default to
an empty Sentry DSN unless CI/local properties provide one explicitly.

You can also pass `BASE_URL` via:

```bash
./gradlew :androidApp:assembleDebug -Papp.baseUrl=https://example.com
```

Secrets and local config files are ignored by Git.

## Dependency Rules

Use these rules when editing Gradle files:

1. Add versions to `gradle/libs.versions.toml`.
2. Prefer bundles for repeated dependency groups.
3. Use convention plugins from `build-logic` instead of repeating common setup.
4. Keep `androidApp` as a thin launcher.
5. Do not add Android UI/Compose/navigation dependencies to domain modules.
6. Do not make feature presentation modules depend on other feature presentation modules.
7. Prefer domain contracts over presentation-to-data dependencies when migrating.

## Testing and Verification

Narrow commands:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:testDebugUnitTest
./gradlew :feature:running-presentation:compileDebugKotlin
```

Broader commands:

```bash
./gradlew productionArchitectureCheck
./gradlew :androidApp:lintRelease
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:bundleRelease
```

Connected/device commands:

```bash
./gradlew :androidApp:connectedDebugAndroidTest
./gradlew :baselineprofile:connectedBenchmark
```

Use the narrowest useful task while iterating. Run broader verification before
handoff when changing build logic, module boundaries, app packaging, release
configuration, or shared dependencies.

## Gradle Profiler

This repo has a checked-in scenario file:

```text
gradle-profiler.scenarios
```

Current scenarios:

| Scenario | Task |
|---|---|
| `architecture_check` | `productionArchitectureCheck` |
| `compose_app_compile_debug` | `:composeApp:compileDebugKotlinAndroid` |
| `compose_app_unit_tests` | `:composeApp:testDebugUnitTest` |

Run all scenarios:

```bash
gradle-profiler --benchmark \
  --project-dir . \
  --scenario-file gradle-profiler.scenarios \
  --output-dir profile-out
```

Run one scenario:

```bash
gradle-profiler --benchmark \
  --project-dir . \
  --scenario-file gradle-profiler.scenarios \
  --output-dir profile-out \
  compose_app_compile_debug
```

Outputs:

```text
profile-out/benchmark.csv
profile-out/benchmark.html
profile-out/profile.log
```

The profiler uses an isolated Gradle user home by default in this setup:

```text
gradle-user-home/
```

The first run is slower because it downloads Gradle and fills dependency/build
caches. Compare measured iterations, not the first warm-up.

## Troubleshooting

### Configuration cache problems

The build currently uses:

```properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

If a task behaves oddly, compare with:

```bash
./gradlew <task> --no-configuration-cache
```

### Dependency resolution issues

Inspect dependencies:

```bash
./gradlew :androidApp:dependencies
./gradlew :composeApp:dependencies
./gradlew :feature:running-data:dependencies
```

For Android-specific dependency reports:

```bash
./gradlew :androidApp:androidDependencies
```

### Kotlin metadata mismatch

Several modules use `-Xskip-metadata-version-check` because the migration graph
can pull newer metadata transitively. Treat this as compatibility debt, not a
pattern to spread without reason.

### Slow first build

Expected causes:

```text
Gradle distribution download
dependency download
Kotlin daemon startup
KAPT/Room setup
configuration cache miss
isolated gradle-profiler user home
```

Run the task a second time before judging incremental performance.

### Release signing fallback

If `androidApp/src/key.properties` is absent, release builds use debug signing
for testing. Do not use that artifact for Play Store release.

## Maintenance Checklist

When changing Gradle configuration:

1. Keep versions in `gradle/libs.versions.toml`.
2. Prefer `build-logic` convention plugins for repeated behavior.
3. Keep `androidApp` launcher-only.
4. Run `./gradlew productionArchitectureCheck`.
5. Run the narrow compile/test task for touched modules.
6. Run `:androidApp:assembleDebug` or `:androidApp:bundleRelease` for packaging changes.
7. Update this document when module shape, convention plugins, or core commands change.
