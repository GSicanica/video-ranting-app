# YouTube Rating App

YouTube Rating App is a production Android/Kotlin project built around a modular Compose architecture. The app is also an active experiment in using modern AI tools to accelerate large-scale software engineering while keeping production quality, reviewability, and maintainability as first-class goals.

The goal of this project is not only to ship a real-world application, but also to explore how modern AI tools can accelerate large-scale software engineering while maintaining production quality.

I would highly appreciate:

- Code reviews
- Architecture feedback
- Performance suggestions
- Best practice recommendations
- General improvements and refactoring ideas

## Google Play

https://play.google.com/store/apps/details?id=com.youtube.rating.android

## Tech Stack

- Kotlin
- Jetpack Compose
- Android Architecture Components
- Kotlin Multiplatform modules
- Coroutines and Flow
- Modular feature architecture
- MVVM/MVI presentation patterns
- Koin dependency injection

## Repository Scope

This public repository contains the Android/KMP client code. Backend code and local/generated build artifacts are intentionally excluded from GitHub.

## Architecture

The current direction is feature-first modularization:

- `androidApp` is the thin launcher module with the single real Android `MainActivity`.
- `composeApp` hosts the main Compose app shell and shared Android UI integration.
- `core:*` modules hold shared domain, data, presentation, and design-system code.
- `feature:*` modules isolate feature data, domain, and presentation layers.
- `shared` contains Kotlin Multiplatform shared code.

## Build Documentation

See [docs/GRADLE.md](docs/GRADLE.md) for the project-specific Gradle guide,
including module layout, convention plugins, key tasks, build properties,
release packaging, iOS framework tasks, and Gradle Profiler usage.

## Feedback

Constructive feedback is welcome, especially around architecture boundaries, performance, Compose best practices, modularization, testing strategy, and practical refactoring opportunities.
