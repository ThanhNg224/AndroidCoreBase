# Changelog

All notable changes to the published `:core` library are recorded here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html) against `:core`'s **public** API
(anything `internal` is not part of the contract and can change in a patch release).

## [Unreleased]

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

[Unreleased]: https://github.com/ThanhNg224/AndroidCoreBase/compare/v1.0.0...HEAD
[v1.0.0]: https://github.com/ThanhNg224/AndroidCoreBase/releases/tag/v1.0.0
