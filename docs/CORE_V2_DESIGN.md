# AndroidCoreBase v2 Design

## Status

Proposed design for the approved breaking `v2.0.0` direction. This document defines the
architecture, public contracts, runtime behaviour, publication model, and verification gates that
the implementation plan must preserve after design review.

## Goals

- Keep the repository cloneable as a complete XML-first Android starter.
- Publish reusable capabilities as independently selectable libraries.
- Keep one umbrella `:core` artifact for consumers that prefer a single dependency.
- Make module boundaries enforce ownership instead of relying only on package conventions.
- Follow current Android guidance for UDF, lifecycle-aware state, opt-in library behaviour,
  resilient persistence, release optimization, and measured performance.
- Prefer a smaller supported API over compatibility with the existing `v1` API.
- Make every release claim reproducible through CI or an explicit device verification command.

## Non-goals

- Preserving source or binary compatibility with `v1.x`.
- Creating feature-specific abstractions in core.
- Shipping a database, analytics vendor, crash reporter, or concrete authentication endpoint.
- Splitting every package into its own Gradle module.
- Treating code coverage or APK size as proof of runtime performance.
- Automatically installing or running work on a user's physical device.

## Repository Products

The repository produces two related products from one source tree:

1. `:app` is a cloneable starter and the executable integration sample.
2. `:core` and its child modules are the published library product.

The starter consumes the same project modules that are published. No production implementation is
copied between `:app` and a library module. A separate Maven consumer smoke build verifies the
published metadata and compiled public API rather than substituting project dependencies.

## Module Topology

```text
:app
  -> :core                           starter default; umbrella dependency

:core                               empty Android umbrella artifact
  -> api(:core:foundation)
  -> api(:core:data)
  -> api(:core:ui-xml)

:core:foundation                    Kotlin/JVM library

:core:data                          Android library
  -> api(:core:foundation)

:core:ui-xml                        Android library
  -> api(:core:foundation)

:core:ui-compose                    optional Android library
  -> api(:core:ui-xml)

:baselineprofile                    Android test module targeting :app

integration/consumer                independent Gradle build using Maven coordinates
```

The umbrella deliberately excludes `:core:ui-compose`; XML consumers must not receive Compose by
default. Consumers opt in to Compose interop explicitly.

One version is used by every published artifact. The `v2.0.0` coordinates are:

```text
com.github.ThanhNg224:AndroidCoreBase:v2.0.0
com.github.ThanhNg224:AndroidCoreBase-foundation:v2.0.0
com.github.ThanhNg224:AndroidCoreBase-data:v2.0.0
com.github.ThanhNg224:AndroidCoreBase-ui-xml:v2.0.0
com.github.ThanhNg224:AndroidCoreBase-ui-compose:v2.0.0
```

## Module Responsibilities

### `:core:foundation`

This module contains only framework-independent contracts and deterministic logic:

- `DomainResult<T>` and domain-safe `AppError` categories.
- `AppDispatchers` and its default coroutine dispatcher implementation.
- `MonotonicClock` contract. Android and JVM adapters live in the modules or tests that can provide
  a platform-appropriate monotonic time source.
- Typed `SettingsKey<T>` and the `SettingsStore` contract.
- `SecureStoreKey` and the `SecureStore` contract.

It must not depend on Android, AndroidX, Retrofit, OkHttp, Hilt, Timber, Material, Compose, or
resource IDs. The generic `UseCase` interface and the `UiState`, `UiEvent`, and `UiEffect` marker
interfaces are removed. Use cases are ordinary focused classes only when they contain reused or
non-trivial application logic.

### `:core:data`

This module owns Android data and transport implementations:

- Preferences DataStore adapter.
- Android Keystore-backed secure storage.
- Authentication session and token refresh coordination.
- Retrofit call execution and transport-to-domain error mapping support.
- OkHttp client factory and file transfer implementation.
- Android connectivity observation for UI hints, not request preflight blocking.

It exposes constructors or explicit factories. It does not install unqualified global Hilt
bindings for `OkHttpClient`, `Retrofit`, logging, or authentication. A consuming app owns client
qualifiers, endpoint selection, interceptor order, and its DI graph.

### `:core:ui-xml`

This module owns XML/ViewBinding presentation infrastructure:

- Edge-to-edge Activity and ViewBinding host support.
- Lifecycle-aware Flow collection.
- Fragment, dialog, and bottom-sheet ViewBinding lifecycle support.
- XML design tokens and reusable Material components.
- App theme and locale adapters.
- Typed Bundle/Intent argument access where it still reduces unsafe casts.
- Monotonic click debouncing.

It does not contain domain results, networking, storage implementations, WorkManager examples,
automatic error dialogs, generic business-action Activities, or automatic process-wide logger
installation.

### `:core:ui-compose`

This module contains only optional XML/Compose interoperability:

- `ComposeView` lifecycle-safe content installation.
- A Material 3 theme bridge based on the `ui-xml` design tokens.
- Compose host helpers that are genuinely shared by starter screens.

No Compose dependency is exported by the umbrella artifact.

### `:app`

The starter owns all app policy and examples:

- Application startup and Hilt composition root.
- App-specific `ApiConfig`, Retrofit service, auth refresher, and logging policy.
- WorkManager configuration and example Worker.
- App shell and Navigation graph.
- Settings and demo vertical slices.
- Weather and design-system showcase code.
- Backup and data-extraction policy.

## UI Architecture

Feature ViewModels extend AndroidX `ViewModel` directly. Each screen exposes one immutable
`StateFlow<ScreenUiState>` and accepts intent through named methods or a screen-owned sealed
`ScreenAction`. Core does not force marker interfaces.

ViewModel-originated work is reduced to state. Core does not expose a Channel- or SharedFlow-based
one-shot effect abstraction. Transient UI messages and navigation requirements are represented by
state with stable IDs and explicit acknowledgement when replay is inappropriate.

For language selection:

1. UI sends `LanguageSelected`.
2. ViewModel calls the language-setting use case immediately.
3. The repository persists/applies the locale.
4. ViewModel updates state to the committed language and, if needed, a transition request ID.
5. UI renders/acknowledges that state.

The generic `TransitionActivity` is removed. Settings becomes a Fragment destination in the
single-Activity navigation graph. Configuration change or process recreation must not repeat a
business action.

Persistent error state renders persistent UI. A dialog or Snackbar is never launched directly by
a generic `ResultState` renderer each time a lifecycle collector restarts. The existing generic
`ResultState`, automatic full-screen overlay binding, and automatic prompt presentation are
removed from the public core API. Screens model loading/content/error as part of their own state.

## Domain and Feature Boundaries

Compile-time dependencies for a classic repository-in-domain feature are:

```text
presentation -> domain <- data
```

Runtime request flow is:

```text
UI -> ViewModel -> optional UseCase -> Repository contract -> Repository implementation -> DataSource
```

The documents must never describe runtime flow as source-code dependency direction.

The domain layer is optional. A use case is created only when it contains business logic, is reused
by multiple ViewModels, or materially simplifies a ViewModel. Pass-through getter/setter/observer
use cases are removed. Domain models do not contain Android resource IDs or Android/AndroidX types.
Display labels are mapped in presentation.

The starter keeps features in `:app` while it remains small. A feature receives its own Gradle
module only after a real second application, independent team boundary, or build-time measurement
justifies it.

## DataStore and Startup Reliability

`DataStoreSettingsStore.observe` catches `IOException` before mapping and emits empty preferences,
allowing typed key defaults to take effect. Non-I/O failures are rethrown. Unit tests cover both
branches.

Theme and locale initialization are app-owned and explicit. Library manifests do not register
process-wide startup initializers. The starter initializes theme from its Application composition
root and uses a bounded splash condition:

- Persisted theme success applies that theme.
- I/O failure applies `SYSTEM` and records only non-sensitive diagnostic metadata.
- The splash condition has a finite fallback and cannot remain true indefinitely.

Core does not plant Timber trees. The starter may plant one tree after checking whether its chosen
logging system has already been initialized.

## Secure Storage

Secure values are stored under `Context.noBackupFilesDir`, not normal SharedPreferences. A single
small encrypted store file is updated through `AtomicFile` so writes either complete or preserve
the previous value. The complete payload is encrypted with AES-256-GCM using a non-exportable
Android Keystore key and a new random 96-bit IV for every write.

The implementation:

- Serializes only string key/value pairs.
- Rejects malformed or truncated payloads without crashing the process.
- Distinguishes cancellation from expected storage failures.
- Serializes read-modify-write operations with a coroutine `Mutex`.
- Never logs keys, values, ciphertext, IVs, tokens, or passphrases.
- Uses 32 random bytes from `SecureRandom` for database passphrases instead of UUID text.

Because the file is under `noBackupFilesDir`, consumers do not need a fragile app-specific backup
exclusion for core secrets. The starter backup rules still exclude any app-owned sensitive files.

## Network and Authentication

Core exposes an explicit factory instead of installing a global Retrofit/OkHttp graph. The factory
accepts timeouts and caller-supplied interceptors but never creates a body logger. Logging policy is
owned by the app; the starter uses no body logging and redacts all authentication/session headers.

The connectivity interceptor is removed. Connectivity state is inherently racy and is only exposed
as advisory UI state; the actual request result classifies `IOException` as a network failure.

Authentication uses a cached session:

- The first token request loads encrypted storage at most once per process.
- Subsequent request interception reads an in-memory snapshot.
- Token updates atomically persist and update the snapshot.
- Concurrent 401 responses share one refresh through a `Mutex`.
- Refresh uses a separate client supplied by the app and never recursively invokes the authenticated
  client.
- Cancellation is always rethrown.
- Retry count is bounded to one retry after the original 401.

The Authorization scheme remains caller-owned: the stored/provider value is the complete header
value, such as `Bearer <token>`.

## File Transfer Contract

Expected operational failures are values; coroutine cancellation and programmer errors are not.

```kotlin
sealed interface TransferEvent<out T> {
    data class Progress(val bytesTransferred: Long, val totalBytes: Long?) : TransferEvent<Nothing>
    data class Completed<T>(val value: T) : TransferEvent<T>
    data class Failed(val error: TransferError) : TransferEvent<Nothing>
}

sealed interface TransferError {
    data class Http(val code: Int) : TransferError
    data class Network(val cause: IOException) : TransferError
    data class FileSystem(val cause: IOException) : TransferError
    data object EmptyBody : TransferError
}
```

Every operation emits exactly one terminal `Completed` or `Failed` event and then completes
normally. It never emits `Failed` and then throws the same expected exception. Cancellation is
re-thrown after resources and partial output are cleaned up.

- `stream` requires `chunkSizeBytes > 0` before returning a Flow.
- Download uses `AtomicFile`; an existing destination survives a failed replacement.
- Upload returns HTTP status metadata only and never reads an unbounded response body into memory.
- Progress delivery is conflated or sampled so slow collectors do not create unbounded memory or
  stall network I/O.
- Tests cover empty bodies, HTTP errors, disconnects, cancellation, invalid chunk size, destination
  write failure, atomic replacement, and concurrent collection rules.

## Performance

R8 code optimization and optimized resource shrinking remain enabled for release. Keep rules are
minimal and are validated against the starter's minified release build and the Maven consumer
smoke build.

The Baseline Profile generator covers cold startup and the starter's critical user journey. Its
generated profile is committed under the app release source set. A Macrobenchmark compares:

- `CompilationMode.None()` cold startup.
- `CompilationMode.Partial` with the Baseline Profile required.

The benchmark records TTID and frame timing on a physical device or supported benchmark emulator.
No fixed percentage improvement is required across all hosts; regressions are evaluated against
the recorded median and trace evidence. CI has a manually triggered or scheduled device workflow;
normal pull requests continue to run deterministic JVM/static/release checks.

## Quality Gates

The normal CI gate runs:

```text
./gradlew check
./gradlew :app:assembleRelease
./gradlew :core:assembleRelease
./scripts/verify-publication.sh
```

Requirements:

- App and library Lint run on release variants with `abortOnError = true` and
  `checkReleaseBuilds = true`.
- Existing Lint warnings are fixed or narrowly documented; broad disabling is prohibited.
- KtLint and Detekt run for production and JVM-test sources.
- Kover verifies at least 80% line coverage over an explicitly documented JVM-testable surface.
  Documentation must not call that filtered number whole-module coverage.
- Each published module uses explicit API mode where supported and has a committed Metalava API
  signature checked by `check`.
- JVM tests cover deterministic logic and failure contracts.
- Instrumentation tests cover Keystore/AtomicFile secure storage, Activity/Fragment integration,
  WorkManager wiring in the starter, and locale/theme configuration changes.
- The Maven consumer build compiles against artifacts from a temporary repository, not
  `mavenLocal()` and not project dependencies.

## Publication and Consumer Experience

All artifacts publish sources and POM metadata from the same version. The umbrella POM exports only
foundation, data, and XML UI. The Compose interop coordinate remains opt-in.

README presents two entry paths:

1. Clone starter: rename application ID/package and replace samples incrementally.
2. Library consumer: select the umbrella or individual artifacts, configure app-owned DI/startup,
   and follow the minimal examples.

The `v2` migration guide explicitly lists removed `v1` APIs and their replacements. No deprecated
compatibility shim is retained solely to preserve the old architecture because there are no real
consumers.

## Removed v1 Concepts

- Generic `UseCase<P, R>` interface.
- `UiState`, `UiEvent`, and `UiEffect` marker interfaces.
- Channel-backed `StateViewModel<S, E, F>` effect delivery.
- Generic `ResultState` UI rendering and automatic error dialogs.
- `TransitionActivity` and its action multibinding.
- Core-owned Timber startup initializer and release tree.
- Core-owned global unqualified Hilt network graph.
- Connectivity request-blocking interceptor.
- SharedPreferences-backed encrypted store.
- Core `HeartbeatWorker` reference implementation.
- Compose dependencies from the umbrella artifact.

## Delivery Sequence

1. Establish module skeleton, common versioning, publication, and Maven consumer smoke build.
2. Move and simplify foundation contracts with API gates.
3. Redesign data/storage/network/auth/transfer through test-first changes.
4. Redesign XML UI and optional Compose interop; migrate Settings to single-Activity state flow.
5. Move app-owned startup, logging, WorkManager, and sample wiring into `:app`.
6. Enable strict release quality gates and device-test workflow.
7. Generate and measure the Baseline Profile on an authorized device environment.
8. Reconcile README, architecture documents, module catalogue, changelog, and migration guide.
9. Run the complete build, publication, API, minified-consumer, and device verification matrix.

## Acceptance Criteria

- A clean clone builds the debug and minified release starter.
- The starter depends on the umbrella project and exercises its principal APIs.
- An independent build compiles using the temporary-repository umbrella coordinate.
- An independent build can select `foundation`, `data`, or `ui-xml` without unrelated optional
  dependencies; Compose appears only when explicitly selected.
- No library manifest performs process-wide logging, theme, locale, network, or work initialization.
- DataStore read I/O failure cannot hold the splash indefinitely.
- Critical state mutations do not depend on one-shot event delivery.
- File transfer has one deterministic terminal-result contract and preserves destination integrity.
- Secure data is stored under no-backup storage and malformed ciphertext cannot crash the app.
- Release Lint, API checks, formatting, static analysis, filtered coverage, R8 build, and Maven
  consumer smoke checks pass.
- The generated Baseline Profile contains starter/core rules and a benchmark report records its
  measured impact.
