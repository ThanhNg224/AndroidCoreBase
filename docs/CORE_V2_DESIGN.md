# AndroidCoreBase v2 Design

## Status

Revised proposal for the approved breaking `v2.0.0` direction. This version incorporates the first
design review and deliberately reduces the published topology from five artifacts to two. It must
be approved before the implementation plan is written.

## Decision Summary

The repository remains one monorepo with two user-facing products:

1. `:app` is the cloneable XML-first starter and executable integration sample.
2. `:core` plus optional `:core:ui-compose` are the published library product.

Only two library artifacts are published in v2:

```text
:core                reusable non-Compose Android library
:core:ui-compose     optional Compose interoperability
```

`foundation`, `data`, and `ui-xml` remain package boundaries inside `:core`, not Gradle modules.
This is intentional for the current measured size of approximately 3,162 Kotlin lines and 79
production files. Module boundaries will be reconsidered only when an observed consumer or build
problem justifies their ongoing publication and verification cost.

## Goals

- Keep the repository cloneable as a complete XML-first Android starter.
- Publish reusable capabilities without forcing Compose on XML-only consumers.
- Make library behaviour opt-in and keep application policy in `:app`.
- Correct the verified startup, persistence, authentication, transfer, and API design problems.
- Follow unidirectional data flow without imposing generic screen marker interfaces.
- Keep releases reproducible through local, CI, temporary-repository consumer, and explicit device
  verification gates.
- Prefer a smaller supported API over compatibility with the existing `v1` API.

## Non-goals

- Preserving source or binary compatibility with `v1.x`.
- Splitting packages into Gradle modules without measured need.
- Publishing a standalone Kotlin/JVM foundation artifact in v2.
- Shipping a database, analytics vendor, crash reporter, or concrete authentication endpoint.
- Treating code coverage, dependency count, or APK size as proof of runtime performance.
- Automatically installing or running work on a user's physical device.

## Why Two Published Modules

Compose currently represents 89 Kotlin lines in three files, but `:core` exports the Compose BOM,
Compose UI, and Material 3 with `api`. Removing that transitive dependency from every XML-only
consumer is an immediate, measurable dependency-hygiene improvement.

Further separation is deferred because the proposed framework-independent surface is currently
about 158 lines, while every published module adds build configuration, publication metadata, API
tracking, quality gates, consumer verification, documentation, and release support. R8 already
removes unused release code, so this decision does not claim a material APK-size win.

Keeping packages stable makes a later split source-compatible through the umbrella artifact, but it
does not make the work free: source roots, resources, tests, `internal` visibility, manifests, DI,
POM dependencies, and publication checks would still move. A later split is justified when at least
one of these is observed:

- A real consumer needs only data or only XML UI and the unwanted dependency graph is material.
- Measured incremental or clean build time identifies `:core` as a bottleneck.
- `:core` grows beyond roughly 8,000-10,000 Kotlin lines and ownership becomes unclear.
- Separate teams or release cadences require compile-time boundaries.
- Android-free contracts need a real JVM or multiplatform consumer.

Until then, package ownership plus automated architecture checks provide the cheaper boundary.

## Repository Topology

```text
:app
  -> implementation(:core)
  -> implementation(:core:ui-compose) only for the Compose showcase

:core
  reusable non-Compose Android library

:core:ui-compose
  -> api(:core)

:baselineprofile
  Android test module targeting :app

integration/consumer
  independent Gradle build using temporary Maven coordinates

build-logic
  included Gradle build containing shared convention plugins
```

The published coordinates are:

```text
com.github.ThanhNg224:AndroidCoreBase:v2.0.0
com.github.ThanhNg224:AndroidCoreBase-ui-compose:v2.0.0
```

The starter consumes project dependencies. The independent consumer resolves both artifacts from a
temporary Maven repository and must never substitute project dependencies or `mavenLocal()`.

## Build Logic

Before adding the second published module, shared build configuration moves to the included
`build-logic` build. Convention plugins centralize:

- Android and Kotlin compiler versions and strict compiler options.
- KtLint, Detekt, Lint, and Kover defaults.
- Release publication metadata and common version resolution.
- Metalava `apiDump` and `apiCheck` wiring for each published Android library.
- Sources JAR and deterministic temporary-repository publication.

Module build files retain only their namespace, features, dependencies, resource prefix, and
module-specific quality exceptions. The existing Metalava `JavaExec` block must not be copied into
another module. Convention plugins are used instead of root `allprojects` or `subprojects`
configuration.

## Module Responsibilities

### `:core`

`:core` owns only reusable, non-Compose Android capabilities:

- Framework-independent dispatcher and storage contracts.
- Preferences DataStore and Android Keystore-backed secure storage implementations.
- Network call execution, authentication coordination, and file transfer.
- XML/ViewBinding lifecycle hosts, design tokens, and reusable Material components.
- Theme and locale adapters.
- Typed Bundle/Intent access and click debouncing.

It does not own:

- Application startup policy or manifest initializers.
- Hilt modules, Hilt qualifiers, or an unqualified global network graph.
- Logging initialization or a release logging tree.
- WorkManager examples.
- Feature use cases, navigation destinations, automatic error dialogs, or business-action
  Activities.
- Compose dependencies or Compose source.

### `:core:ui-compose`

This optional module owns only:

- `ComposeView` lifecycle-safe content installation.
- A Material 3 theme bridge backed by `:core` XML design tokens.
- A Compose Activity host only if the starter has a real reusable need for it after migration.

It exports Compose because its public API contains Compose types. It exports `:core` so its resource
and lifecycle contracts resolve for consumers. It contains no duplicate colors or dimensions.

### `:app`

The starter owns all policy and executable examples:

- `Application` startup and Hilt composition root.
- Hilt provider/binding modules for core constructors and factories.
- App-specific endpoint, Retrofit service, auth refresher, and logging policy.
- WorkManager configuration and example Worker.
- App shell, navigation graph, Settings, Demo, and design-system showcase.
- Backup and data-extraction policy.

The starter demonstrates both direct construction and Hilt wiring so library consumers can choose
their own DI framework.

## Dependency Injection Contract

Both published modules are DI-framework agnostic:

- They do not apply the Hilt or KSP plugins.
- They do not depend on `hilt-android` or Hilt qualifiers.
- Public implementations use explicit constructors or factories without `@Inject` annotations.
- They do not install anything into a consumer's component graph.

`:app` remains a Hilt application and explicitly provides the required core objects. This adds a
small amount of consumer setup compared with v1, but prevents hidden global bindings, qualifier
collisions, and mandatory annotation processing. README includes a minimal Hilt module and a direct
construction example.

Removing Hilt must not make implementation classes public by default. The v2 exposure policy is:

| Capability | Public v2 surface | Hidden or removed implementation |
|---|---|---|
| Coroutine dispatchers | `AppDispatchers` and `AppDispatchers.default()` | `DefaultAppDispatchers` remains internal |
| Preferences storage | `SettingsKey`, `SettingsStore`, and `SettingsStore.from(dataStore)` | `DataStoreSettingsStore` remains internal |
| Secure storage | `SecureStore`, `SecureStoreKey`, and `SecureStore.encrypted(context, dispatchers)` | `EncryptedFileSecureStore` remains internal |
| API execution | `ApiClient`, `ApiResult`, and `NetworkClientFactory.createApiClient()` | `RetrofitApiClient` remains internal |
| File transfer | `FileTransferClient`, events/errors, and `NetworkClientFactory.createFileTransferClient(...)` | `OkHttpFileTransferClient` remains internal |
| Authentication | Public constructor for `AuthSession`; `AuthTokenProvider`; `AuthTokenRefresher`; `NetworkClientFactory.createAuthenticator(...)` | Session provider and single-flight authenticator implementations remain internal |
| Theme | `AppTheme`, `ThemeManager`, and `ThemeManager.create(settingsStore)` | `AndroidThemeManager` remains internal |
| Locale | Public constructors for `LocaleManager` and `AppCompatLocaleApplier` | Startup context holder is removed |
| Database passphrase | Public constructor for `DbPassphraseProvider` | Generation and memoization details remain private |
| String resources | No provider abstraction in core | `StringProvider` and `AndroidStringProvider` are removed as unused |
| Monotonic clock | No core API in v2 | `ElapsedRealtimeClock` and its unused implementation are removed |
| Connectivity | No core API in v2 | Checker, blocking interceptor, exception, and `ACCESS_NETWORK_STATE` usage are removed |

Focused companion factories are preferred for storage and UI contracts. `NetworkClientFactory` is
the one public subsystem factory for OkHttp/Retrofit/auth/transfer construction; it must not become
a general service locator. `ActivityNavigator` loses its `@Inject` annotation and is removed if no
consumer remains after Settings becomes a Fragment. Metalava records only the public column.

## Resource Contract

`:core` keeps `resourcePrefix = "core_"`. Existing public tokens such as `core_space_16` retain
their names in v2 unless a separate focused resource migration proves a name invalid. App layouts
and `docs/DESIGN_SYSTEM.md` continue to reference those names.

`:core:ui-compose` defines no duplicate token resources. Its Kotlin theme bridge reads resources
from `:core` through the module dependency.

## Manifest Contract

Published manifests are passive and contain no permissions, components, providers, services, or
initialization metadata:

- `INTERNET` moves to the starter manifest because only the application can decide to use network
  capabilities.
- `ACCESS_NETWORK_STATE` is removed with the unused connectivity API.
- AndroidX Startup providers and all core initializers are removed.
- `TransitionActivity` is removed.
- AppCompat locale auto-storage metadata, if retained, is declared by `:app`, not the library.

README lists `INTERNET` as a required consumer manifest entry only when the network APIs are used.
The independent consumer asserts the merged manifests of both published artifacts contain none of
the entries above.

## Framework-independent Source Boundary

Framework-independent contracts live only under
`com.thanhng224.androidcorebase.core.foundation`. A build-logic verification task fails when that
source root imports `android.*`, `androidx.*`, Retrofit, OkHttp, Hilt, Material, Compose, or Android
resources. Coroutines and the Kotlin/JDK standard libraries are allowed. Metalava protects the
public API shape but is not treated as an Android-dependency boundary check.

`com.thanhng224.androidcorebase.core.ui.text.UiText` is explicitly Android presentation code: it
may hold `@StringRes` IDs and is outside the framework-independent import guard. It may be used by
app presentation state such as `PendingMessage`, but it must never appear in feature domain models.

The generic `UseCase<P, R>`, `UiState`, `UiEvent`, and `UiEffect` marker interfaces are removed.
Use cases are ordinary focused classes only when they contain reused or non-trivial application
logic. Feature ViewModels extend AndroidX `ViewModel` directly.

## Feature-owned Domain Errors and Transport Errors

Core defines neither a universal `AppError` nor a generic `DomainResult`. The current generic result
has only one real consumer, the starter's weather sample. Publishing a two-parameter result would
also require a permanent operator algebra (`map`, `mapError`, `flatMap`, `fold`, and error widening)
for a domain layer that the architecture explicitly makes optional. That is unnecessary public API.

When a feature needs typed business failure, it defines a finite feature-owned result, for example:

```kotlin
sealed interface WeatherError {
    data object Unavailable : WeatherError
    data object InvalidResponse : WeatherError
}

sealed interface WeatherResult {
    data class Success(val weather: Weather) : WeatherResult
    data class Failure(val error: WeatherError) : WeatherResult
}
```

Features that do not need a typed failure return their value directly. Unexpected programmer errors
still throw and cancellation is always rethrown.

The network layer has a separate technical contract:

```kotlin
sealed interface ApiFailure {
    data class Http(val code: Int, val serverMessage: String?) : ApiFailure
    data class Network(val cause: IOException) : ApiFailure
    data class Serialization(val cause: Throwable) : ApiFailure
    data object EmptyBody : ApiFailure
}

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiFailure) : ApiResult<Nothing>
}
```

A feature data mapper converts `ApiFailure` into its feature-owned result/error. Presentation never
switches on HTTP codes or exceptions. Cancellation and programmer errors are rethrown, not
converted into `ApiResult`.

## UI State and Transient Request Protocol

Each screen exposes one immutable `StateFlow<ScreenUiState>` and accepts intents through named
methods or a screen-owned sealed action. Core does not publish a generic one-shot effect stream.

Persistent work is always represented by ordinary state. For transient messages or navigation
that originate in a ViewModel, the screen owns an explicit queued request protocol. The following
is the required shape, not a new core marker hierarchy:

```kotlin
data class PendingMessage(
    val id: Long,
    val text: UiText,
    val actionLabel: UiText? = null,
    val action: MessageAction? = null,
)

sealed interface MessageAction {
    data object ResetCounter : MessageAction
}

data class DemoUiState(
    val count: Int = 0,
    val pendingMessages: List<PendingMessage> = emptyList(),
)
```

The protocol is deterministic:

1. Each ViewModel owns an `AtomicLong(0)` request counter. `incrementAndGet()` allocates an ID; wall
   clock and `nanoTime` are never used. IDs are unique across concurrent coroutines within that
   ViewModel instance, and gaps have no meaning.
2. The request is appended with `MutableStateFlow.update`; FIFO order is the order in which those
   atomic state updates succeed, not numeric ID order.
3. The UI presents only `pendingMessages.firstOrNull()`.
4. Snackbar dismissal calls `onMessageHandled(id)`; its action button calls
   `onMessageAction(id)`.
5. The ViewModel removes only a matching current head. A stale or duplicate acknowledgement is a
   no-op.
6. Action acknowledgement removes the message before executing the action, preventing replay.
7. A second message waits in the list and cannot overwrite the first.
8. If configuration changes before acknowledgement, the same request remains in ViewModel state
   and the recreated UI may present it. A transient queue is not restored after process death; any
   operation that must survive process death belongs in persisted domain state or WorkManager.

Navigation uses the same ID and acknowledgement rules in a screen-owned nullable request. The UI
acknowledges only after `NavController.navigate` succeeds and ignores the request when already at
the target destination. Navigation that is a direct consequence of durable business state is
derived from that state instead of queued as a transient request.

The starter includes ViewModel tests for ordering, duplicate acknowledgement, action handling, and
configuration-style re-collection. No reusable `ConsumableEvent` wrapper is introduced.

## Settings and Locale Mutation

Settings becomes a Fragment in the single-Activity navigation graph. `TransitionActivity`, its
action multibinding, manifest entry, resources, and tests are removed.

Language selection does not require a transient request:

1. UI calls `selectLanguage(language)`.
2. ViewModel persists the language through its repository.
3. On successful persistence, the app-owned locale adapter calls
   `AppCompatDelegate.setApplicationLocales`.
4. Android performs the required configuration recreation.
5. State is sourced again from persisted locale/settings data.

A failed write leaves the previous selected language in state and queues a screen-owned error
message. Tests prove that persistence happens before locale application and that re-collection does
not repeat the mutation.

## DataStore and Startup Reliability

`DataStoreSettingsStore.observe` catches upstream `IOException` before mapping and emits empty
preferences so typed defaults apply. Non-I/O failures are rethrown. Unit tests cover both paths.

Theme and locale startup are app-owned and explicit. Library manifests register no process-wide
initializers. The starter applies theme and locale from its Application composition root and uses a
bounded splash condition:

- Persisted state success applies that state.
- I/O failure applies safe defaults and records only non-sensitive diagnostic metadata.
- A finite fallback guarantees that splash readiness cannot remain false indefinitely.

Core does not plant Timber trees. The starter owns logging initialization and never logs payloads,
tokens, identifiers, images, or other sensitive content.

## Secure Storage

Secure values live under `Context.noBackupFilesDir`, not SharedPreferences. A single small encrypted
store file is updated through `AtomicFile`, so a failed write preserves the previous complete value.
The whole payload is encrypted with AES-256-GCM using a non-exportable Android Keystore key and a
new random 96-bit IV on every write.

The implementation:

- Stores only string key/value pairs.
- Serializes read-modify-write operations with a coroutine `Mutex`.
- Rejects malformed or truncated payloads without crashing the process.
- Rethrows cancellation and returns typed failures for expected storage errors.
- Never logs keys, values, ciphertext, IVs, tokens, or passphrases.
- Uses 32 bytes from `SecureRandom` for database passphrases instead of UUID text.

Because core secrets use `noBackupFilesDir`, consumers do not need a fragile app-specific backup
exclusion for them. The starter still excludes app-owned sensitive files.

## Network and Authentication

Core exposes explicit factories instead of a global Retrofit/OkHttp graph. A factory accepts
timeouts and caller-supplied interceptors but never creates a body logger. The starter logging
policy uses no body logging and redacts authentication and session headers.

The connectivity request-blocking interceptor is removed. Connectivity observation is advisory UI
state only because preflight connectivity checks are racy; the actual request result classifies
`IOException` as a network failure.

Authentication keeps the existing single-flight and bounded-retry behaviour and adds the missing
session cache:

- The first token request loads encrypted storage at most once per process.
- Subsequent interception reads an in-memory snapshot.
- Token updates persist first and then atomically update the snapshot.
- Concurrent 401 responses share one refresh through a `Mutex`.
- Refresh uses an app-supplied separate client and cannot recursively invoke the authenticated
  client.
- Cancellation is always rethrown.
- At most one retry follows the original 401.

The stored/provider value is the complete Authorization header value, such as `Bearer <token>`.

## File Transfer Contract

All three operations use one explicit event algebra. `Payload` exists specifically so streaming
can deliver chunks without confusing them with terminal success:

```kotlin
sealed interface TransferEvent<out P, out R> {
    data class Progress(
        val bytesTransferred: Long,
        val totalBytes: Long?,
    ) : TransferEvent<Nothing, Nothing>

    data class Payload<P>(val value: P) : TransferEvent<P, Nothing>
    data class Completed<R>(val value: R) : TransferEvent<Nothing, R>
    data class Failed(val error: TransferError) : TransferEvent<Nothing, Nothing>
}

sealed interface TransferError {
    data class Http(val code: Int) : TransferError
    data class Network(val cause: IOException) : TransferError
    data class FileSystem(val cause: IOException) : TransferError
    data object EmptyBody : TransferError
}

typealias DownloadEvent = TransferEvent<Nothing, File>
typealias UploadEvent = TransferEvent<Nothing, HttpTransferMetadata>
typealias StreamEvent = TransferEvent<ByteArray, Unit>
```

Operation rules:

- Download emits progress and exactly one terminal `Completed(destination)` or `Failed`.
- Upload emits progress and exactly one terminal `Completed(metadata)` or `Failed`.
- Stream emits zero or more `Payload(chunk)` events followed by exactly one terminal
  `Completed(Unit)` or `Failed`.
- A terminal event completes the Flow normally; the same expected failure is never both emitted and
  thrown.
- Cancellation is rethrown after resources and partial output are cleaned up.
- Programmer errors such as `chunkSizeBytes <= 0` fail synchronously before a Flow is returned.
- Unknown content length is represented by `null`, never a negative total.
- Download uses `AtomicFile`; an existing destination survives a failed replacement.
- Upload exposes status metadata only and never buffers an unbounded response body.
- Progress is conflated or sampled so a slow collector cannot create unbounded memory or stall
  network I/O.
- Each returned Flow is cold and starts an independent HTTP call per collection; this is documented
  and tested.

Tests cover empty bodies, HTTP errors, disconnects, cancellation, invalid chunk size, destination
write failure, atomic replacement, stream payload ordering, terminal cardinality, and repeated
collection.

## Performance

R8 code optimization and optimized resource shrinking remain enabled for release. Keep rules are
minimal and verified against the starter's minified release build and temporary Maven consumer.

The Baseline Profile generator covers cold startup and the starter's critical user journey. Its
generated profile is committed under the app release source set. Macrobenchmark compares
`CompilationMode.None()` with `CompilationMode.Partial` where the Baseline Profile is required, and
records TTID plus frame timing on an authorized physical device or supported benchmark emulator.

No fixed percentage improvement is promised across hosts. Results are judged by median and trace
evidence. Normal pull requests run deterministic checks; a manually triggered or scheduled device
workflow owns benchmark execution.

## Published Consumer Verification

`integration/consumer` is a standalone Android Gradle build with its own `settings.gradle.kts`; it
is not included as a project in the root build. It has two dependency modes selected only by the
verification script:

- Main artifact only, which compiles an XML host and asserts Compose is absent.
- Main artifact plus `AndroidCoreBase-ui-compose`, which compiles the Compose interoperability host.

`scripts/verify-publication.sh` creates a temporary directory, publishes both release artifacts into
a temporary Maven repository, runs the consumer with only that repository plus Google/Maven
Central, inspects POMs and merged manifests, and removes the directory on exit. It rejects
`mavenLocal()`, project substitution, unexpected Hilt/Compose dependencies, and library-provided
permissions/components.

The script has two explicit modes:

- `--quick`: publish, inspect metadata/manifests, and compile both consumer debug variants. It runs
  on every pull request.
- `--release`: perform all quick checks plus minified consumer release builds and R8 verification.
  It runs on `main`, release tags, manual pre-release CI, and the final local release gate.

No CI job downloads a previously published v2 artifact for this proof; every run tests the artifacts
produced by the checked-out commit.

## Quality and Coverage Gates

The normal deterministic gate runs:

```text
./gradlew check
./gradlew :app:assembleRelease
./gradlew :core:assembleRelease
./gradlew :core:ui-compose:assembleRelease
./scripts/verify-publication.sh --release
```

Requirements:

- Release Lint uses `abortOnError = true` and `checkReleaseBuilds = true`.
- Existing warnings are fixed or narrowly documented; broad disabling is prohibited.
- KtLint and Detekt cover production and JVM-test sources.
- Metalava API signatures are committed and checked for both published artifacts.
- The framework-independent package import guard runs under `check`.
- `:core` enforces at least 80% line coverage only for its explicitly listed deterministic,
  JVM-testable classes. The task and documentation call this `deterministic-core coverage`, not
  whole-module coverage.
- `:core:ui-compose` has no line-coverage percentage gate because it is lifecycle/UI glue. It is
  covered by Lint, API checks, compilation, host instrumentation, and the consumer build.
- XML components and lifecycle hosts use focused instrumentation tests instead of a misleading
  filtered unit-coverage percentage.
- Keystore/AtomicFile storage, locale/theme configuration, WorkManager app wiring, and host
  lifecycle integration have instrumentation coverage.
- Pull requests run `verify-publication.sh --quick`; `main`, pre-release, and release gates run
  `verify-publication.sh --release`.

## Delivery Checkpoints

Every checkpoint is buildable and reviewable; there is no long-running branch state in which all
gates are knowingly broken.

1. **Build and dependency checkpoint**
   - Introduce `build-logic` conventions.
   - Remove core Hilt/KSP usage and manifest pollution, add the approved public factories, and move
     application bindings/permissions/locale metadata to `:app`.
   - Extract `:core:ui-compose` and remove Compose from `:core`.
   - Only after the Hilt-free artifacts compile, establish both publications, API checks, and the
     quick temporary-repository consumer proof.
2. **Library correctness checkpoint**
   - Redesign transport results, DataStore recovery, secure storage, auth caching, networking, and
     file transfer through test-first commits.
   - Remove startup, logging, and Worker policy from published modules.
3. **Starter architecture checkpoint**
   - Move composition policy and Worker example to `:app`.
   - Replace `StateViewModel`/effects and migrate Settings to the single-Activity flow.
   - Update app ViewModel and instrumentation tests.
4. **Release proof checkpoint**
   - Enable strict release gates, generate/measure the Baseline Profile, and reconcile all docs.
   - Run the complete local/publication/consumer/device matrix.

No intermediate `v1.x` tag is planned because there are no real consumers and the user explicitly
authorized breaking changes. Each checkpoint is nevertheless kept green so a compatible subset
could be released separately if that constraint changes.

## Removed v1 Concepts

- Generic `UseCase<P, R>` and UI marker interfaces.
- Channel-backed generic `StateViewModel` effect delivery.
- Universal `AppError` and generic `DomainResult` contracts.
- Generic `ResultState` rendering and automatic error dialogs.
- `TransitionActivity` and action multibinding.
- Core-owned Timber startup initializer and release tree.
- Core-owned Hilt modules, qualifiers, and global unqualified network graph.
- Connectivity request-blocking interceptor.
- Connectivity checker/exception, `StringProvider`, and unused elapsed-realtime clock abstractions.
- SharedPreferences-backed encrypted store.
- Core `HeartbeatWorker` reference implementation.
- Compose dependencies and source in the main artifact.

## Acceptance Criteria

- A clean clone builds the debug and minified release starter.
- `:app` uses `:core` for DataStore settings, secure storage, network/auth, transfer, XML lifecycle
  hosts, theme/locale adapters, and design tokens.
- `:app` uses `:core:ui-compose` only for its explicit Compose showcase.
- Pull requests compile both temporary-repository consumer modes; main/pre-release/release gates
  additionally build both minified consumer releases.
- The main `AndroidCoreBase` POM contains no Compose dependency.
- No published manifest performs process-wide logging, theme, locale, network, or work
  initialization.
- Published manifests contain no permissions or application components; the starter explicitly
  owns its `INTERNET` and locale metadata.
- Published artifacts contain no Hilt dependency, Hilt annotation, generated Hilt code, or Hilt
  manifest metadata.
- Framework-independent packages fail verification on an Android or transport import.
- The Metalava API contains only the approved contracts, factories, and public constructors from
  the DI exposure table; concrete implementations remain absent.
- DataStore read I/O failure cannot hold the splash indefinitely.
- Settings tests prove persistence precedes locale application and lifecycle re-collection cannot
  repeat the mutation.
- Message protocol tests prove FIFO ordering, action handling, and idempotent acknowledgement.
- File transfer tests prove payload ordering, exactly one terminal event, cancellation propagation,
  and destination integrity.
- Secure-storage instrumentation proves no-backup location, atomic replacement, and malformed
  payload recovery.
- Release Lint, API checks, formatting, static analysis, deterministic-core coverage, R8 build, and
  temporary Maven consumer checks pass.
- The committed Baseline Profile contains starter/core rules and a benchmark report records its
  measured impact.
