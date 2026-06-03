---
name: android-kmp-mega-skill
description: |
  Unified architecture and implementation skill for Android + Kotlin Multiplatform in Codex CLI.
  Covers module structure, data layer, typed error handling, DI with Koin, type-safe navigation,
  presentation MVI, Compose UI patterns, and testing.
  Trigger on requests like: "create feature", "set up module", "repository/data source",
  "Room/Ktor", "Result/DataError", "Koin module", "Compose navigation", "ViewModel MVI",
  "Compose UI", "write tests with Turbine/JUnit".
---

# Android / KMP Mega Skill (Codex CLI)

## Scope

Use this skill for Android/KMP architecture and implementation tasks in Codex CLI when work involves one or more of:
- Module structure and dependency boundaries
- Data layer patterns (Room/Ktor/repositories/mappers)
- Typed error handling with `Result` + domain errors
- Dependency injection with Koin
- Type-safe Compose navigation
- Presentation MVI (State/Action/Event + ViewModel)
- Compose UI quality/performance patterns
- Unit/UI testing strategy and examples

## When Not To Use

Do not prioritize this skill for:
- Backend-only services with no Android/KMP client scope
- Infra/devops-only tasks unrelated to app architecture
- One-off scripts or throwaway prototypes where full architecture rules are explicitly out of scope

---

## 1) Module Structure

- Feature-first modularization, then per-layer split (`presentation -> domain <- data`).
- Features must not depend on each other directly.
- Shared cross-feature concepts go to `core:*` modules.
- Prefer convention plugins and version catalog; avoid hardcoded versions.

Default layout guidance:
- `:app`
- `:build-logic`
- `:core:domain`, `:core:data`, `:core:presentation`, `:core:design-system`
- `:feature:<name>:domain`, `:feature:<name>:data`, `:feature:<name>:presentation`

Dependency rules:
- `presentation` may depend on own `domain` + core presentation/domain/design-system
- `data` may depend on own `domain` + core domain/data
- `domain` depends only on core domain

---

## 2) Data Layer

- Use **data source** for single source access; use **repository** only when coordinating multiple sources.
- Keep DTO/Entity/Domain model separation strict.
- Map via explicit extension mappers (`Dto <-> Domain`, `Entity <-> Domain`).
- Name implementations by behavior/source (`RoomXDataSource`, `KtorXDataSource`, `OfflineFirstXRepository`).
- Configure Ktor client centrally and inject it.
- Prefer offline-first behavior where meaningful.

---

## 3) Error Handling

- Use typed `Result<T, E>` and `EmptyResult<E>`.
- Maintain explicit error taxonomies (e.g., `DataError.Network`, `DataError.Local`) in domain/core.
- Prefer extension helpers for flow (`map`, `onSuccess`, `onFailure`, `asEmptyResult`).
- Convert low-level exceptions to typed errors as close as possible to source.

---

## 4) DI (Koin)

- One Koin module per feature-layer where bindings exist.
- Assemble modules in app startup, not ad hoc in feature code.
- Prefer constructor-reference style (`singleOf`, `viewModelOf`, `factoryOf`) when supported; fallback to lambda when needed.
- In Compose roots, inject ViewModels at root boundary (`koinViewModel()` pattern).

---

## 5) Navigation (Compose)

- Use type-safe routes (`@Serializable` route objects/data classes).
- One nav graph per feature presentation module.
- Compose app-level NavHost assembles feature graphs.
- Cross-feature navigation via callbacks to avoid feature coupling.
- Pass IDs/scalars in routes; load complex objects in destination ViewModel.

---

## 6) Presentation MVI

Per screen:
- Single immutable `State` data class
- Sealed `Action` (user intents)
- Sealed `Event` (one-time effects)
- ViewModel owns `StateFlow<State>` and emits events via `Channel`/flow bridge

Rules:
- UI renders state and forwards actions; no business logic in composables.
- Update state with `copy` and atomic updates.
- Keep screen composables thin; move logic to ViewModel/domain.
- Use lifecycle-aware state collection.

---

## 7) Compose UI

- Prioritize stability and predictable recomposition.
- Keep app state in ViewModel; use `remember` for compose-owned state only.
- Use side effects only when necessary; prefer ViewModel-driven flows.
- Use keys in lazy lists when stable IDs exist.
- Prefer animation techniques that avoid unnecessary recompositions.
- Maintain accessibility basics (content descriptions, semantics).

---

## 8) Testing

- Unit tests: JUnit + Turbine + coroutine test dispatcher.
- Prefer fakes over heavy mocks for repositories/data sources.
- Test ViewModel state evolution and one-time events.
- Use `SavedStateHandle` directly in tests where applicable.
- Add Compose UI tests for high-value interaction paths.

---

## Priority Rules (Conflict Resolution)

When sections overlap, apply this precedence:
1. Correctness and typed error safety (Error Handling)
2. Architecture boundaries (Module Structure, Data Layer)
3. Runtime wiring safety (DI, Navigation)
4. State model consistency (Presentation MVI)
5. UI polish/performance guidance (Compose UI)
6. Testing strategy and coverage optimization (Testing)

Specific overlap rule:
- If MVI and Compose-side-effect guidance conflict, prefer MVI state/action ownership first; Compose side effects are fallback only for UI/runtime integration that cannot live in ViewModel.

---

## Trigger Phrases (Activation Hints)

- "create feature/module", "project structure", "convention plugin"
- "repository", "data source", "Room", "DAO", "Ktor", "DTO", "mapper"
- "Result", "DataError", "onSuccess/onFailure", "typed error"
- "Koin module", "inject ViewModel", "startKoin"
- "navigation", "route", "NavGraph", "cross-feature navigation"
- "MVI", "State Action Event", "ViewModel screen architecture"
- "Compose recomposition", "LazyColumn", "side effects", "design system"
- "write test", "Turbine", "SavedStateHandle", "Compose UI test"

---

## Output Expectations For Codex

For implementation tasks:
- Keep architecture decisions explicit and consistent with this skill.
- Prefer small, reviewable changes with focused tests.
- State assumptions when requirements are underspecified.

For review tasks:
- Prioritize boundary violations, regression risks, and missing tests.
- Keep summaries short; findings first.

---

## Single Source Of Truth

- Canonical file: `~/.codex/skills/android-kmp-mega-skill/SKILL.md`
- Draft sources in `/Users/goran/Downloads/skills` are reference-only.
- Do not edit `SKILL copy*.md` as runtime skill definitions.

## Change Log / Update Process

- Update only the canonical mega skill file.
- For each update, append a short dated note here:
  - `2026-04-19`: Initial mega-skill consolidation from `SKILL.md`, `SKILL copy*.md`, and relevant `README.md` context.
- If a rule changes behavior, also update `Trigger Phrases` and `Priority Rules` in the same edit.
