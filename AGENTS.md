# AGENTS.md - YouTube Rating App

## Project Rules

This app is treated as a production Android/KMP codebase. Preserve runtime behavior first, then improve structure incrementally.

Read the detailed rule files before changing code in those areas:

| File | Covers |
|---|---|
| `.cursor/rules/architecture.mdc` | Module ownership, package boundaries, Koin wiring, feature migration, verification |
| `.cursor/rules/android-compose-design-system.mdc` | Compose UI conventions, reusable components, themes, accessibility |
| `.cursor/rules/dependency-updates.mdc` | Safe dependency updates and version catalog usage |
| `.cursor/rules/testing.mdc` | Unit, Compose, instrumentation, and smoke test expectations |
| `.cursor/rules/contributions.mdc` | Git workflow, PR notes, and validation evidence |

## Current Shape

The app currently uses an incremental migration structure:

```text
:androidApp   thin Android application launcher, manifest, signing, variants
:composeApp   primary Android/KMP UI and Android feature implementation
:shared       KMP shared data/network/domain code
:build-logic  Gradle convention plugins
```

`androidApp` intentionally compiles only launcher sources and depends on `composeApp`. Most app functionality still lives in `composeApp/src/androidMain`. Do not move large feature sets in one step unless the migration is covered by focused tests.

## Architecture Direction

Target structure follows feature-first boundaries:

```text
core/
  core-domain/
  core-data/
  core-presentation/
  core-design-system/

feature/
  ratings/
    ratings-domain/
    ratings-data/
    ratings-presentation/
  home/
    home-domain/
    home-data/
    home-presentation/
```

Until modules are split physically, enforce the same boundaries by package:

```text
com.youtube.rating.android.domain
com.youtube.rating.android.data
com.youtube.rating.android.ui
com.youtube.rating.android.navigation
com.youtube.rating.android.di
```

## Dependency Rules

- UI/presentation may depend on domain contracts and app design-system components.
- Domain must not depend on Android UI, Compose, navigation, Room, OkHttp, DataStore, or Koin.
- Data may implement domain contracts and use network/local storage dependencies.
- Feature packages must not reach directly into another feature's implementation. Share through domain contracts, shared core APIs, or app-level navigation callbacks.
- `androidApp` remains a thin composition/packaging module. Do not add app logic there.
- Add dependencies through `gradle/libs.versions.toml`; avoid hardcoded versions in build files.

## DI Rules

- Use Koin modules as the app composition root.
- Bind interfaces to implementations in DI, not from UI code.
- Prefer constructor injection and ViewModel injection at screen roots.
- Do not call `KoinJavaComponent.get()` from random UI code for new work.
- Avoid creating standalone `CoroutineScope(SupervisorJob() + Dispatchers.IO)` in feature code; inject or centralize app scopes/dispatchers when touching that code.

## UI Rules

- Build screens with existing reusable components under `ui/components` before introducing one-off widgets.
- Keep screen composables thin: render state, forward actions, and collect events.
- Keep business logic in ViewModels/use cases/repositories.
- Use stable keys for lazy lists.
- Provide content descriptions for interactive icons and media controls.

## Build & Test Commands

```bash
./gradlew productionArchitectureCheck
./gradlew :composeApp:testDebugUnitTest
./gradlew :androidApp:lintRelease
./gradlew :androidApp:assembleDebug
```

Use the narrowest useful command while developing, then run the broader verification before handing off larger changes.

