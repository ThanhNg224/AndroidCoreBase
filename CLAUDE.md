# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project State (read this first)

This repo is a **reusable Android XML base project**, not a finished product app. It is an **XML + ViewBinding, MVVM + Clean Architecture** base; `docs/ARCHITECTURE.md`, `docs/CORE_MODULES.md`, `docs/FEATURE_TEMPLATE.md`, and the source tree are the current source of truth.

The starter Compose surface has been removed from `app`: ViewBinding is enabled, layouts live under `app/src/main/res/layout/`, and the current source tree already includes the core packages described in `docs/ARCHITECTURE.md`, `docs/CORE_MODULES.md`, `docs/FEATURE_TEMPLATE.md`, and `docs/DESIGN_SYSTEM.md`. Treat those docs and the real source under `app/src/main/java/com/example/androidxmlbase/` as the current state before adding new core modules or feature code.

Reference project for generic ideas only (do not copy blindly, and never touch this path):
```
/Users/thanhng224/Dev/Kalapa/heyjapan-android-main
```

Before changing the base architecture, read `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/STANDARD.md`, and `docs/CORE_MODULES.md`. Extract only generic, modern ideas from the reference project; never bring product logic into this base.

## Current Phase: Base-Building (scoped YAGNI exception)

This repo is in the **base-building phase**: hardening the reusable foundation (`core/`) itself, not shipping a feature on top of it. In this phase, proactively adding modern, production-grade infrastructure to `core/` — startup correctness, security posture, logging, performance tooling — is expected even before a concrete consuming feature demands it, because gaps in the base are expensive to retrofit once features depend on it.

This is a narrow exception to "don't add abstractions without a proven need" (see the working conventions below and `AGENTS.md`). It does **not** license:
- New Gradle modules containing `app`/feature source code unless explicitly requested after a real module boundary is justified.
  - Exception: build-tooling/test-only modules that contain no business or feature code (e.g. a `:baselineprofile` module holding only Macrobenchmark tests) are allowed, since they don't fork the app's architecture or add a real module boundary to maintain — they're closer to `androidTest` than to a feature module.
- Feature-specific or business-domain abstractions.
- Anything that isn't genuinely foundational (i.e., not something every consuming app would eventually need).

Once feature development on top of this base starts, the strict "proven need" rule applies again in full.

## Commands

```bash
./gradlew :app:assembleDebug        # build debug APK
./gradlew :app:assembleRelease      # build release APK
./gradlew test                      # run JVM unit tests (app/src/test)
./gradlew check                     # full local gate: unit tests, lint, ktlint, detekt, Kover coverage
./gradlew :app:ktlintFormat         # auto-format Kotlin where ktlint can safely fix
./gradlew connectedAndroidTest      # run instrumented tests (app/src/androidTest), needs device/emulator
./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.SomeTest"   # run a single unit test class
./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.SomeTest.someMethod"  # run a single test method
```

Quality gates are part of the base: Android lint, ktlint, detekt, and Kover coverage are wired into `check`. Kover enforces 80%+ line coverage on the explicitly unit-testable core/domain/data/viewmodel surface, excluding Android UI glue, Hilt generated code, and generated databinding/R classes.

## Architecture

Two Gradle modules: `:app` (application, feature/sample code, package `com.example.androidxmlbase`) and `:core` (the reusable foundation — architecture, DI, network, storage, UI toolkit — package `com.thanhng224.androidxmlbase.core`, published to JitPack; see `README.md` for consumption instructions and `docs/CORE_MODULES.md` for its internal layout). `:app` depends on `:core` via `implementation(project(":core"))`; `:core` must never depend on `:app` or on feature/sample code. Do not introduce further Gradle module splits (e.g. `:core:network`, `:core:ui`) unless asked and a real module boundary is justified.

Dependency direction:
```
UI -> ViewModel -> UseCase -> Repository interface -> RepositoryImpl -> DataSource/API/DB/Storage
```

Layer rules (from `docs/ARCHITECTURE.md`, `docs/STANDARD.md`, `AGENTS.md`):
- **Presentation**: renders UI, observes state, handles input/navigation. No API/DB access, no business rules.
- **Domain**: business rules, UseCases, entities, repository *interfaces*. No Android framework dependency.
- **Data**: repository *implementations*, remote/local data sources, mappers. No UI logic; never leak API/DB models to Presentation.
- Dependencies point inward only (Presentation → Domain → Data is the call direction; Data may depend on Domain only for repository interfaces). Feature modules must not depend on each other directly; shared/core code must not depend on feature code, and only moves into `core`/`shared` after proven reuse (2+ cases).

Per-feature folder structure (see `docs/FEATURE_TEMPLATE.md` for the worked example):
```
feature/<name>/
  data/{datasource,dto,mapper,repository}/
  domain/{entity,repository,usecase}/
  presentation/{model,viewmodel,ui}/
```

Current core folders are documented in `docs/CORE_MODULES.md`. Do not assume aspirational folders such as `core/analytics`, `core/navigation`, or `core/logging` exist until the source tree actually has them.

UI state convention: explicit `UiState` / `UiEvent` / `UiEffect`, one-way data flow, one-time effects (navigation, toasts) delivered via `UiEffect` and never as sticky state.

Dependency injection convention: Hilt is the app-wide DI framework. Use constructor injection by default, `@HiltViewModel` for ViewModels, `@AndroidEntryPoint` on concrete Activities/Fragments, and small Hilt modules only for interface bindings or Android/framework object creation.

### Do not port into this base

Even though the reference HeyJapan project is the source of ideas, never bring across: its lesson/learning-content domain, IAP/paywall/sale logic, ads-specific logic, DBFlow legacy setup, concrete Firebase/Facebook analytics clients, the legacy singleton `Preference`, large mixed-responsibility helpers, or feature-specific remote config keys. Copy only a generic, clean, modern concept; rewrite a strong idea whose implementation is app-specific, weakly typed, or legacy; drop product-specific code.

## Working conventions (from AGENTS.md / docs/STANDARD.md)

- Before adding any Activity, Fragment, ViewModel, UseCase, Repository, Mapper, Adapter, Dialog, or Custom View, search the codebase for an existing one to extend/reuse first.
- Naming: classes `PascalCase`; functions/variables `camelCase`; constants `UPPER_SNAKE_CASE`; resources/layouts/drawables/colors/dimens `lowercase_with_underscores`.
- Kotlin: prefer `val`, avoid `!!`, use sealed classes for finite UI state, avoid `GlobalScope`, use structured concurrency and `viewModelScope`.
- No hardcoded non-zero `dp`/`sp` in XML layouts — use the fixed spacing/radius/size/text-size tokens in `core/src/main/res/values/dimens.xml` (`@dimen/core_space_<n>`, `core_radius_<n>`, `core_size_<n>`, `core_stroke_width`, `core_text_size_<n>`; see `docs/DESIGN_SYSTEM.md`). A value that is genuinely local and decorative — not a design-system step other screens should reuse — belongs in the consuming module's own `dimens.xml` with a semantic name (see `app/src/main/res/values/dimens.xml`'s `shimmer_line_width_*`), not bolted onto `:core`'s shared scale. This base does **not** use `com.intuit.sdp`/`com.intuit.ssp` — retired in Phase 2 of `docs/MODERNIZATION.md` (D1). No hardcoded hex colors in layouts except launcher assets. All user-facing text goes through string resources.
- Keep refactors scoped to what's requested; don't rewrite unrelated files or introduce abstractions without a repeated, proven need (base-infrastructure hardening in `core/` is exempted — see "Current Phase" above).
