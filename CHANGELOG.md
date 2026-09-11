# Changelog

All notable changes to the published `:core` library are recorded here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html) against `:core`'s **public** API
(anything `internal` is not part of the contract and can change in a patch release).

## [Unreleased]

## [v2.0.1] - 2026-09-11

Republish of the withdrawn `v2.0.0` with no source changes to the published API.

### Fixed

- `jitpack.yml` still ran `:core:publishToMavenLocal` from the v1 layout, so JitPack produced only
  `com.github.ThanhNg224:AndroidCoreBase` and never the second v2 coordinate,
  `AndroidCoreBase-ui-compose`, even though the README and this changelog advertise it. Both
  modules are now published. `scripts/verify-publication.sh` could not catch this: it publishes to
  its own temporary Maven repository and never reads `jitpack.yml`.

**Use `v2.0.1`. `v2.0.0` is withdrawn.** Its tag has been deleted from the repository, but
deleting a tag does not unpublish anything: JitPack caches builds per version, so
`com.github.ThanhNg224:AndroidCoreBase:v2.0.0` still resolves (verified, HTTP 200) and will keep
resolving. It is an incomplete release --
`com.github.ThanhNg224:AndroidCoreBase-ui-compose:v2.0.0` does not exist and never can. Nothing
consumed it; the tag lived for under an hour.

### Changed -- the JitPack group changed

Publishing a second module changes how JitPack names **both** of them:

| | `v2.0.0` | `v2.0.1` |
|---|---|---|
| Main | `com.github.ThanhNg224:AndroidCoreBase` | `com.github.ThanhNg224.AndroidCoreBase:AndroidCoreBase` |
| Compose interop | not published | `com.github.ThanhNg224.AndroidCoreBase:AndroidCoreBase-ui-compose` |

Once a build publishes more than one module, JitPack namespaces every module under
`com.github.<user>.<repo>` and repurposes the short `com.github.ThanhNg224:AndroidCoreBase`
coordinate as an **aggregator POM depending on both modules**. It still resolves, so an upgrade
that changes only the version silently starts pulling `ui-compose`, and with it Compose, into an
XML-only app. Change the group as well as the version.

`scripts/verify-publication.sh` cannot catch this either: it publishes to a plain temporary Maven
repository, where the coordinates are exactly what the Gradle publication declares. The rename is
JitPack-specific.

## v2.0.0 - 2026-09-09 -- withdrawn

Tag deleted; superseded by `v2.0.1`. Kept here because its JitPack artifact is permanently cached
and because everything below is the actual v2 change set, which `v2.0.1` ships unchanged.

Breaking release. `:core` and the new `:core:ui-compose` module are now dependency-injection-agnostic:
neither applies a Hilt/KSP plugin or ships DI annotations. See `docs/MIGRATION_V1_TO_V2.md` for the
complete removed-API-to-replacement table and `docs/CORE_V2_DESIGN.md` for the full decision record.

### Added
- **New published module `AndroidCoreBase-ui-compose`**: `AndroidCoreBaseTheme`, `ComposeView.setThemedContent`, and `BaseComposeActivity` moved out of the main artifact into this optional, Compose-only module.
- **Public factories replacing every Hilt binding `:core` used to provide**: `AppDispatchers.default()`, `SettingsStoreFactory.create(dataStore)`, `SecureStoreFactory.encrypted(context, dispatchers)`, `ThemeManager.create(settingsStore)`, `NetworkClientFactory.createApiClient()`/`createFileTransferClient(...)`/`createAuthTokenProvider(...)`/`createAuthenticator(...)`/`createOkHttpClient(...)`/`createRetrofit(...)`, plus public constructors for `AuthSession`, `DbPassphraseProvider`, `LocaleManager`, and `AppCompatLocaleApplier`.
- **Metalava API tracking for `:core:ui-compose`** (`core/ui-compose/api/ui-compose.api`), alongside `:core`'s existing gate.
- **`verifyDeterministicCoreCoverage`** Gradle task: a stable alias for `koverVerify`, enforcing `:core`'s Kover coverage rule over an explicit, positively-selected deterministic (non-UI) package list instead of a wildcard-plus-growing-exclusion-list.
- **Isolated publication proof** (`scripts/verify-publication.sh`, `integration/consumer/`): publishes both artifacts to a throwaway Maven repository and builds a standalone consumer project against them in both main-only and Compose modes, asserting the main POM/manifest carry no Compose, Hilt, or test-only dependency.
- **Strict release Lint on every module**, including `:app` (previously `abortOnError = false`).

### Changed
- **Settings moved into the single-Activity navigation graph** as `SettingsFragment`, reached via `NavController` from the shared top app bar. Selecting a language now persists through the repository first, then applies via `LocaleManager` directly — no activity-recreating transition.
- **Every screen's transient one-shot request is now acknowledged state**, not a `Channel`-backed effect: `StateViewModel<S, E, F>`/`UiEffect` gave way to a plain `ViewModel` + `StateFlow<S>` with a `pendingMessages: List<PendingMessage>` field on `S`.
- **`ApiResult`'s failure cases renamed and restructured** into `ApiFailure` (`Http`, `Network`, `Serialization`, `EmptyBody`); empty-body detection now uses HTTP 204/205 status instead of a null-body check (OkHttp 5's `Response.body` is non-nullable).
- **`TokenAuthenticator` allows at most one retry** (was two).
- **`EncryptedSecureStore` renamed to `EncryptedFileSecureStore`**, now writing through `AtomicFile` so a failed write can never corrupt the previously committed value.
- **`testFixturesApi(junit)`/`testFixturesApi(kotlinx-coroutines-test)` downgraded to `testFixturesImplementation`**, so they no longer leak into the main published POM.
- Published manifests (`:core`, `:core:ui-compose`) are now fully passive (`<manifest />`): no permissions, components, providers, or services. `INTERNET` and AppCompat's locale auto-storage metadata are declared by the consuming app.

### Removed
- **Core Hilt modules** (`core/di/AppCoreModule`, `core/di/NetworkModule`, `core/di/CoroutineScopeModule`, `core/ui/theme/ThemeModule`) — replaced by app-owned modules calling the public factories above.
- **`DomainResult`/`AppError`** — replaced by feature-owned result/error types.
- **`ResultState`/`ResultRenderState`/`renderResultState`/`bindResultState`/`FullScreenLoaderView`/`PromptDialogFragment`** — a screen renders its own explicit sealed state instead.
- **`TransitionActivity`/`TransitionAction`** and the action multibinding — see "Settings moved" above.
- **`core/network/connectivity`** (`ConnectivityChecker`, `ConnectivityInterceptor`) and **`core/time/ElapsedRealtimeClock`** — unused in every real consumer.
- **`core/ui/text/StringProvider`/`AndroidStringProvider`** — unused; `UiText.resolve(context)` already covers it.
- **All `androidx.startup` initializers** (`TimberInitializer`, `ThemeApplyInitializer`, `LocaleContextInitializer`, `AppStartupEntryPoint`) — replaced by explicit `Application.onCreate()` orchestration (`AppStartupCoordinator`).
- **`core/logging/ReleaseTree`** and **`core/work/HeartbeatWorker`** — moved to the app as reference shapes, not shipped by `:core`.
- **`core/navigation/ActivityNavigator`/`ActivityDestination`/`NavigationOptions`/`TransitionType`** — their only consumer was the removed `SettingsActivity`.
- **`core.architecture.UseCase<in P, R>`** — a use case is now a plain class with `operator fun invoke`.

## [v1.0.0] - 2026-07-29

First stable public release of `:core` (`com.github.ThanhNg224:AndroidCoreBase:v1.0.0`). Consolidated and hardened base library after full baseline modernization effort.

### Added
- **Enforced public API gate for `:core`**: `./gradlew :core:apiDump` writes `core/api/core.api`; `:core:apiCheck` fails on any undeclared change and is wired into `./gradlew check`. Driven by `com.android.tools.metalava:metalava` (1.0.0-alpha15).
- **R8 minification & resource shrinking enabled**: `:app` release build runs R8 (`isMinifyEnabled = true`, `isShrinkResources = true`), exercising `:core`'s `consumer-rules.pro`. Sample release APK size reduced from 20.49 MB down to 2.71 MB.
- **Compose Interop Support**: `AndroidCoreBaseTheme` bridges XML theme token colors to Compose `MaterialTheme`; `ComposeView.setThemedContent()` handles safe disposal; `BaseComposeActivity` for Compose screens.
- **Edge-to-edge Window Insets handling**: `BaseActivity` enables edge-to-edge by default and applies window insets as padding.
- `AuthTokenRefresher` interface for `TokenAuthenticator` standard token refresh logic.
- `testFixtures` artifact providing unit test doubles (`MainDispatcherRule`, `FakeSecureStore`, `FakeSettingsStore`, `FakeConnectivityChecker`, etc.).
- MIT License included in publication POM.

### Changed
- **Rebranded project**: Package namespace modernized to `com.thanhng224.androidcorebase.core` and artifact ID to `AndroidCoreBase`.
- **Refactored Activity Base Hierarchy**: Neutral `BaseActivity` (MVI + Edge-to-edge), `BaseBindingActivity<VB : ViewBinding>`, and `BaseComposeActivity`.
- **`DbPassphraseProvider` is now public** and lives in `core.storage.secure` (reusable Keystore-backed AES passphrase generator).
- **Fixed spacing & sizing tokens**: Replaced `com.intuit.sdp`/`ssp` continuous scaling with predictable, fixed `core_space_<n>`, `core_radius_<n>`, `core_size_<n>`, `core_text_size_<n>` design tokens.
- `ApiConfig` is supplied by consuming apps via Hilt DI instead of baked-in base URLs.
- `AppLanguage` converted to data class to support custom consumer language lists.
- Public API surface audit: Dependencies exposing types in public signatures set to `api(...)` configuration.

### Removed
- **Unused Room + SQLCipher database stack removed**: Removed `AppDatabase`, `LocalSettingEntity`, `LocalSettingDao`, `DatabaseModule`, and `DbPassphraseWarmupInitializer` along with 7.3 MB native `.so` binaries. Consuming apps declare their own `@Database` if persistence is required.
- **Deprecated `android:statusBarColor`** removed from XML themes in favor of modern system bar inset handling.
- **Removed sdp/ssp dependencies** (`com.intuit.sdp` / `com.intuit.ssp`) and deleted `ResponsiveContextWrapper`.

### Fixed
- Fixed Compose Compiler plugin artifact coordinate resolution for release builds (`compose-group-mapping`).
- Redacted `Authorization` header in OkHttp logging interceptor to avoid leaking bearer tokens.
- `TimberInitializer` respects consumer application's `FLAG_DEBUGGABLE` status instead of AAR build flag.

[Unreleased]: https://github.com/ThanhNg224/AndroidCoreBase/compare/v2.0.1...HEAD
[v2.0.1]: https://github.com/ThanhNg224/AndroidCoreBase/compare/v1.0.0...v2.0.1
[v1.0.0]: https://github.com/ThanhNg224/AndroidCoreBase/releases/tag/v1.0.0
