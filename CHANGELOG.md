# Changelog

All notable changes to the published `:core` library are recorded here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html) against `:core`'s **public** API
(anything `internal` is not part of the contract and can change in a patch release).

## [Unreleased]

### Removed

- **`:core` no longer ships a database.** `AppDatabase`, `LocalSettingEntity`, `LocalSettingDao`,
  `DatabaseModule`, `DbPassphraseWarmupInitializer` and the Room + SQLCipher dependencies are gone.
  All of it was `internal` and consumed by nothing, and it could not be consumed: Room's
  `@Database` fixes its `entities` list at compile time in the annotated class, so a library cannot
  hand a consumer a database to extend. It cost every consuming app ~2 MB of native SQLCipher per
  ABI (7.3 MB across four in a universal APK, which R8 cannot strip) plus Keystore I/O at every
  process start. The `:app` sample's release APK went 20.49 MB → 13.16 MB. If you need an encrypted
  database, declare your own `@Database` and add Room + SQLCipher yourself.

### Added

- **Enforced public API gate for `:core`.** `./gradlew :core:apiDump` writes `core/api/core.api`;
  `:core:apiCheck` fails on any undeclared change and is wired into `check`. Driven by the official
  `com.android.tools.metalava:metalava` (the tool AndroidX uses) rather than the community Gradle
  plugin, which was last released in 2022. Known limitation: metalava runs with only the Android boot
  classpath, so 4 of 885 lines (the reified `intentExtra`/`fragmentArg` delegates) show an unresolved
  return type — see `docs/MODERNIZATION.md` F1.

- **R8 is enabled for `:app`'s release build** (`isMinifyEnabled` + `isShrinkResources`), which also
  exercises `:core`'s `consumer-rules.pro` for the first time. The sample release APK is 2.71 MB,
  down from 20.49 MB at the start of this cycle. Validated on device against a signed minified
  build: live network + kotlinx.serialization, Hilt, DataStore, per-app locale and the Compose
  interop all work. `consumer-rules.pro` needed no additions.

### Changed

- **`DbPassphraseProvider` is now public** and lives in `core.storage.secure` (was `internal` in
  `core.storage.database`). It is the reusable half of the removed database layer: a stable random
  passphrase persisted through `SecureStore`, memoized in memory, for your own SQLCipher
  `SupportFactory`. Because `:core` no longer warms it during startup, deciding where to absorb the
  first (disk-reading) call is now the consumer's — see its KDoc.
- `:core`'s `consumer-rules.pro` no longer keeps `core.storage.database.**`; that Room keep rule
  belongs in the build of whichever app declares the database.

## [v2.0.0]

First release that a project outside this repository can realistically consume. `v1.0.0`
published an artifact, but several things only worked because the in-repo `:app` happened to
redeclare the same dependencies and supply its own WorkManager configuration.

### Breaking

- **`ApiConfig` is now supplied by the consuming app.** `:core` no longer ships a base URL
  (`v1.0.0` baked the sample's demo weather API into the AAR through a `buildConfigField`, and
  `ApiConfig` was `internal`, so it could be neither changed nor named). Provide one from your own
  Hilt module; injecting `Retrofit`/`OkHttpClient` without a binding now fails with a message
  showing the module to write. `ApiConfig` also gained per-timeout overrides.
- **`AppLanguage` is a data class, not an enum.** Consumers could not add a language to a closed
  enum. `AppLanguage.ENGLISH`/`VIETNAMESE` still resolve, but `entries`/`values()` and exhaustive
  `when` no longer apply — use `AppLanguage.BUILT_IN`, or supply your own list via the
  `SupportedLanguages` Hilt binding or `LocaleManager`'s constructor.
- **`locales_config` moved to the app.** The locales an app ships are the app's concern; declare
  your own `@xml/locales_config` and point `android:localeConfig` at it.
- **Resources are `core_`-prefixed.** Layouts, anims, drawables, raw assets and styleables were
  renamed (`activity_transition` → `core_activity_transition`, `FrameButton` →
  `CoreFrameButton`, …) so a consumer's same-named resource can no longer silently override
  `:core`'s. Styles/themes keep `Type.AndroidCoreBase.Variant` naming, which is already namespaced.
- **Storage identifiers are namespaced.** The DataStore (`app_settings` → `core_app_settings`),
  database (`app_database.db` → `core_app_database.db`), encrypted-prefs file and KeyStore alias
  were renamed off app-specific names. Existing installs start from empty state for these stores.
- `:core` no longer removes `androidx.work.WorkManagerInitializer` from the merged manifest. If
  your app implements `Configuration.Provider`, add that `tools:node="remove"` entry yourself.

### Added

- `AuthTokenRefresher` — bind your own implementation to make `TokenAuthenticator` actually
  refresh on a 401. `AuthSession` exposes access/refresh token read/write and `clear()`.
- `testFixtures` artifact with reusable doubles: `MainDispatcherRule`, `FakeSecureStore`,
  `FakeSettingsStore`, `FakeConnectivityChecker`, `FakeAuthTokenProvider`,
  `FakeAuthTokenRefresher`, `FakeAppLocaleApplier`. Consume with
  `testImplementation(testFixtures("com.github.ThanhNg224:AndroidCoreBase:<version>"))`.
- `LICENSE` (MIT), referenced from the publication POM. `v1.0.0` shipped with no license.
- CI (`.github/workflows/check.yml`) running the full gate on pushes and PRs, and a `jitpack.yml`
  pinning JDK 21 instead of relying on JitPack's default.

### Changed

- Dependencies whose types appear in `:core`'s public API are now `api(...)` instead of
  `implementation(...)` — Retrofit, OkHttp, coroutines, AppCompat, Fragment, lifecycle-viewmodel,
  Material and Timber. Previously they landed in the POM's runtime scope only, so an external
  consumer could not compile against `ApiClient`, `SettingsStore`, `BaseActivity` or
  `StateViewModel`.
- Widened to public the contracts the README advertises but that were `internal`: `SecureStore`,
  `FileTransferClient`/`TransferResult`, the `intentExtra`/`fragmentArg` delegates,
  `BaseDialogFragment`, `BaseBottomSheetDialogFragment`, `AppDispatchers`, `StringProvider`,
  `ConnectivityChecker`/`NoConnectivityException`, `ShadowLayout`, `FullScreenLoaderView`,
  `PromptDialogFragment`, `ReleaseTree` and friends. Framework implementations behind those
  interfaces stay `internal`.
- Android Lint now runs with `abortOnError`/`checkReleaseBuilds` enabled on the published module;
  both had been disabled.
- Kover measures the whole module instead of a hand-picked ~25-class allowlist, with each
  exclusion stating why. The honest figure is ~82% line coverage; `v1.0.0`'s advertised "80%+"
  described a curated subset.

### Fixed

- `TokenAuthenticator` never refreshed anything: on a 401 it re-read the same cached token twice.
  It now delegates to `AuthTokenRefresher` behind a mutex so parallel 401s share one refresh, and
  gives up after two attempts instead of looping.
- `TimberInitializer` branched on `:core`'s own `BuildConfig.DEBUG`, which is always `false` in a
  published AAR — consumers' debug builds got `ReleaseTree` and lost all `DEBUG`/`INFO` logging.
  It now reads the consuming app's `FLAG_DEBUGGABLE`.
- `AppDatabase` used `fallbackToDestructiveMigration(true)` with no migrations defined, so the
  first schema bump would silently wipe every consuming app's local data. Only downgrades reset now.
- The OkHttp logging interceptor did not redact `Authorization`, so enabling request logging
  printed bearer tokens to Logcat.

### Known gaps

- No binary-compatibility gate. `binary-compatibility-validator` 0.18.1 registers no
  `apiDump`/`apiCheck` tasks for a `com.android.library` module, so it was removed rather than
  left applied doing nothing. Public API changes are currently caught by review plus the
  `internal`-by-default discipline.

## [v1.0.0]

- Initial JitPack publication of `:core`.

[Unreleased]: https://github.com/ThanhNg224/AndroidCoreBase/compare/v2.0.0...HEAD
[v2.0.0]: https://github.com/ThanhNg224/AndroidCoreBase/compare/v1.0.0...v2.0.0
[v1.0.0]: https://github.com/ThanhNg224/AndroidCoreBase/releases/tag/v1.0.0
