# YouTube Rating App Architecture

## Goal

The goal is to move the app toward a production structure without breaking the existing Android app. The current codebase is already mid-migration, so the immediate rule is: keep `androidApp` thin, keep behavior stable, and extract features in small vertical steps.

## Current Modules

```text
:androidApp
```

Owns Android application packaging: manifest, application ID, signing, build variants, release configuration, and launcher source. It depends on `:composeApp`.

```text
:composeApp
```

Owns most current Android app behavior: Compose UI, navigation, Android-specific data, app-level Koin wiring, resources, notifications, widgets, media playback, and migration-hosted legacy code.

It is also the canonical Compose Multiplatform UI module for iOS XCFramework output (`composeApp.xcframework`) consumed by `iosApp`.

```text
:iosComposeApp
```

Owns an experimental secondary iOS Compose module. It is now wired into the root build so its tasks are visible, but it is not the primary runtime path for `iosApp`.

```text
:feature:running-data
```

Owns running feature persistence, domain models, GPS route tracking, and foreground-service support.

```text
:feature:running-presentation
```

Owns running feature UI, ViewModel, and feature-owned navigation/DI assembly.

```text
:shared
```

Owns KMP-safe shared data/network/domain code. Code in `commonMain` must remain platform-neutral.

```text
:build-logic
```

Owns Gradle convention plugins so module configuration is centralized.

```text
included build: kmmApp
```

Owns a separate sandbox KMM application that is now reachable from the root workspace through a composite build, without being merged into the main application graph.

```text
:desktopApp
:webApp
```

Are now valid scaffold modules in the root build. They do not yet own runtime targets.

## Target Modules

Future extraction should move toward:

```text
:core:domain
:core:data
:core:presentation
:core:design-system
:feature:ratings:domain
:feature:ratings:data
:feature:ratings:presentation
:feature:home:domain
:feature:home:data
:feature:home:presentation
```

This mirrors the same production goal as an `api`/`impl` split: public contracts are separated from implementation, and feature implementation details do not leak across the app.

## Migration Strategy

1. Keep `androidApp` as a launcher/composition package.
2. Stabilize package boundaries inside `composeApp`.
3. Extract one feature at a time.
4. Move domain contracts first.
5. Move data implementations second.
6. Move presentation last.
7. Wire the extracted feature from app-level Koin/navigation assembly.

Avoid mixing feature extraction with UI redesign, dependency updates, or behavior changes.

## Verification

Run:

```bash
./gradlew productionArchitectureCheck
```

The task enforces rules that should already be true and reports known migration debt separately.
