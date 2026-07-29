# CORE_MODULES.md

`core/` lives in the `:core` Gradle module under `com.thanhng224.androidcorebase.core`; `:app` consumes it through `implementation(project(":core"))`.

One section per `core/*` package that actually exists in this codebase today (verified against `core/src/main/java/com/thanhng224/androidcorebase/core/` directly, not reconstructed from earlier phase plans). Each section lists the real public API surface and which feature(s) currently consume it. If a class/file isn't listed here, it doesn't exist yet — don't assume it does.

> **Verify before you trust this file.** On 2026-07-29 three claims in the `core/architecture/result` section below were found to be wrong (a `map` extension and an `AppError` variant that did not exist, and a `ResultState.Error` field typed `String` when the code used `UiText`) — despite the paragraph above. They have been reconciled, but the lesson stands: check the source before building a decision on anything here. See `docs/MODERNIZATION.md` finding F2.

`:core` runs in Kotlin **explicit API mode**, covering `src/main` and `src/testFixtures`. Every public declaration therefore carries an explicit `public` modifier and an explicit return type; anything not meant for consumers must be marked `internal`. Implementations stay `internal` behind a public interface with a Hilt binding, so a consumer injects the interface and never names an impl — see the audit table in `docs/MODERNIZATION.md`.

## `core/architecture`

The MVVM primitives every feature is built on. Framework-light: only `StateViewModel` depends on `androidx.lifecycle`.

- `UiState` — empty marker interface. Feature state classes implement it (e.g. `DemoUiState`).
- `UiEvent` — empty marker interface. Feature event sealed interfaces implement it (e.g. `DemoUiEvent`).
- `UiEffect` — empty marker interface for one-shot effects (e.g. `DemoUiEffect`). `DesignSystemViewModel` uses the bare `UiEffect` interface directly (no dedicated effect type) since it never emits one.
- `AppDispatchers` (`main`/`io`/`default` `CoroutineDispatcher`s) + `DefaultAppDispatchers` implementation. Bound in Hilt and used by blocking/IO-heavy adapters.
- `UseCase<in P, R>` — `suspend operator fun invoke(params: P): R`. Implementers: `SaveDemoCountUseCase`, `FetchDemoWeatherUseCase`. See `docs/FEATURE_TEMPLATE.md` section 4 for when to implement it vs. stay a plain class.
- `StateViewModel<S : UiState, E : UiEvent, F : UiEffect>(initialState: S)` (abstract, extends `ViewModel`) — exposes `state: StateFlow<S>`, `effect: Flow<F>` (buffered `Channel`-backed), `protected val currentState: S`, `abstract fun onEvent(event: E)`, `protected fun setState(reducer: S.() -> S)` (implemented via `MutableStateFlow.update {}`, atomic under concurrent calls), `protected fun sendEffect(effect: F)`.

### `core/architecture/result`
- `ResultState<out T>` (sealed interface) — `Loading`, `Success<T>(val data: T)`, `Error(val message: UiText, val cause: Throwable? = null)`. Plus `inline fun <T, R> ResultState<T>.fold(onLoading, onSuccess, onError): R`. `message` is a `UiText` (see `core/ui/text`), not a `String`, so a presentation-layer error can carry an unresolved string resource and be localised at render time.
- `DomainResult<out T>` (sealed interface) — `Success<T>(data)` and `Error(error: AppError)` for domain/data results that should not carry UI strings. Contains `map` extension function to transform Success cases.
- `AppError` (sealed interface) — reusable error categories: `Http(code, serverMessage)`, `Network(cause)`, `Parse(cause)`, `EmptyBody`, and `Business(code, message)`.

**Consumers:** `DemoViewModel`, `DesignSystemViewModel` (both extend `StateViewModel`); `DomainResult`/`AppError` are used by `sample/demo`'s repository/use case path so data/domain can report failure without UI strings; `ResultState` is used by `sample/designsystem` (`DesignSystemUiState.demoResult`) and by `core/ui/base` render helpers. `UseCase<in P, R>` is implemented by `sample/demo`'s `SaveDemoCountUseCase`/`FetchDemoWeatherUseCase`.

**Why three result types, not one:** `ApiResult` (`core/network`), `DomainResult`/`AppError` (here), and `ResultState` (here) look similar but belong to different layers on purpose — `ApiResult` carries Retrofit/HTTP-shaped errors and must not leak past data sources; `DomainResult`/`AppError` are the domain-safe, UI-string-free version repositories/use cases return; `ResultState` is presentation-only and is what a ViewModel exposes to a View. Each layer maps the one below into its own type (see `sample/demo`'s mapper) instead of passing the lower type through. Don't collapse these into one shared type — that would leak Retrofit/HTTP types into the domain or UI layer.

## `core/storage`

### `core/storage/settings`
A typed, testable settings store backed by Jetpack DataStore (`androidx.datastore:datastore-preferences`).
- `SettingsKey<T>` (sealed class, `name`/`defaultValue`) with 5 typed subclasses: `StringKey`, `IntKey`, `LongKey`, `BooleanKey`, `FloatKey`.
- `SettingsStore` (interface) — `fun <T> observe(key): Flow<T>`, `suspend fun <T> get(key): T`, `suspend fun <T> set(key, value)`, `suspend fun <T> remove(key)`.
- `DataStoreSettingsStore(dataStore: DataStore<Preferences>)` — the only implementation. Takes a `DataStore<Preferences>` directly (never a `Context`) so it stays unit-testable on the JVM.
- `Context.appSettingsDataStore` — the `preferencesDataStore(name = "core_app_settings")` delegate; the one place a `Context` is involved, kept out of the testable class.
- `AppSettingsKeys` — exactly 4 app-wide keys: `THEME_MODE` (String, default `"system"`), `FIRST_OPEN_AT` (Long, default `0L`), `OPEN_COUNT` (Int, default `0`), `DEBUG_LOGGING_ENABLED` (Boolean, default `false`).

### `core/storage/secure`
- `SecureStoreKey`, `SecureStore`, `SecureStoreKeys` (all public) — string-secret storage contract for tokens/secrets. Built-in keys: `AUTH_TOKEN`, `REFRESH_TOKEN`.
- `EncryptedSecureStore` (internal) — AES-256/GCM via an Android Keystore key (deliberately not the deprecated `EncryptedSharedPreferences`), behind the `SecureStore` interface. Provided as the app-wide `SecureStore` by Hilt. Prefs file `core_secure_store`, Keystore alias `core_secure_store_key` — `core_`-namespaced so they can't collide with a consuming app's own storage.

**Consumers:** `DemoRepositoryImpl` (sample-private counter key + `SettingsStore`); `SettingsRepositoryImpl` reaches theme persistence through `ThemeManager`; `SecureStoreAuthTokenProvider` reads `SecureStoreKeys.AUTH_TOKEN`.

### `core/storage/database`
- `DbPassphraseProvider` — memoized SQLCipher passphrase resolver (`suspend fun getOrCreate(): String`), backed by `SecureStore`. Warmed on `Dispatchers.IO` during process startup so `DatabaseModule`'s Hilt `@Provides` boundary doesn't block on disk I/O.
- `AppDatabase` (Room, `@Database`, version 1, db file `core_app_database.db`) — SQLCipher-encrypted via `DatabaseModule`'s `SupportOpenHelperFactory`. No `fallbackToDestructiveMigration`: an upgrade with no matching `Migration` crashes instead of silently dropping data; `fallbackToDestructiveMigrationOnDowngrade(true)` only resets on a version downgrade (e.g. after a rollback).
- `LocalSettingEntity` (`@Entity(tableName = "local_settings")`, `key`/`value` string columns) + `LocalSettingDao` (get/observe/save/delete by `key`) — a generic encrypted key-value table; reference shape only, not consumed by any feature yet.

**Consumers:** none yet for `AppDatabase`/`LocalSettingDao` — add a `Migration` object here before bumping `version` past 1.

## `core/network`

- `ApiResult<out T>` (sealed interface) — `Success<T>(data)`, `HttpError(code, message)`, `NetworkError(cause)`, `ParseError(cause)`, `EmptyBody`.
- `ApiConfig(baseUrl, enableLogging = false, connectTimeoutSeconds/readTimeoutSeconds/writeTimeoutSeconds = 30)` (public) — **supplied by the consuming app**, not by `:core`. `NetworkModule` declares it `@BindsOptionalOf`; injecting `Retrofit`/`OkHttpClient` with no binding throws an `IllegalStateException` whose message shows the module to write. `:core` ships no base URL on purpose — a library must not dictate one (`:app` provides its own in `app/.../di/AppNetworkModule.kt`).
- `ApiClient` (interface) — `suspend fun <T> execute(call: suspend () -> retrofit2.Response<T>): ApiResult<T>`.
- `RetrofitApiClient` — the `ApiClient` implementation; classifies success/HTTP error/empty body, catches `IOException` as `NetworkError`, any other `Exception` as `ParseError`, and always rethrows `CancellationException` before those catches.
- `NetworkClientFactory` (internal object) — reusable factory functions for `OkHttpClient` and `Retrofit`; timeouts come from `ApiConfig` and the logging interceptor redacts the `Authorization` header. Named apart from `core/di/NetworkModule` (the Hilt module) so "factory" vs. "DI wiring" stays unambiguous.

### `core/network/auth`
- `AuthSession` (public, constructor-injected) — the gateway a consuming app uses to read/write `SecureStoreKeys.AUTH_TOKEN`/`REFRESH_TOKEN` without depending on the internal `SecureStore` contract directly: `getAccessToken()`, `getRefreshToken()`, `setTokens(accessToken, refreshToken?)`, `clear()`.
- `AuthTokenProvider` (interface, `suspend fun getToken(): String?`) + `SecureStoreAuthTokenProvider` (delegates to `AuthSession.getAccessToken()`) + `NoOpAuthTokenProvider` for tests/demo overrides.
- `AuthTokenInterceptor` — adds token returned by `AuthTokenProvider.getToken()` directly into `"Authorization"` header if not null/blank.
- `AuthTokenRefresher` (public interface, `suspend fun refresh(refreshToken: String?): String?`) — extension point a consuming app implements and binds (its own `@Binds`/`@Provides`) to call its own refresh endpoint. Core ships no implementation and no default binding (`NetworkBindingsModule.bindAuthTokenRefresher` is a `@BindsOptionalOf`), so without one bound `TokenAuthenticator` gives up on a 401 instead of pretending to refresh.
- `TokenAuthenticator` (`okhttp3.Authenticator`) — on a 401, mutex-guards a single in-flight refresh per process: if `AuthSession`'s cached token already differs from the one that just failed (another caller already refreshed), reuses it; otherwise calls the bound `AuthTokenRefresher` and persists the result via `AuthSession.setTokens`. Gives up after 2 retries (via the OkHttp `priorResponse` chain) to avoid infinite 401 loops.

### `core/network/connectivity`
- `ConnectivityChecker` (interface, `fun isConnected(): Boolean`) + `AndroidConnectivityChecker(context)` (real impl via `ConnectivityManager`).
- `ConnectivityInterceptor` — throws `NoConnectivityException` (an `IOException`) before any request leaves the device if `ConnectivityChecker.isConnected()` is false.

### `core/network/transfer`
- `FileTransferClient` + `OkHttpFileTransferClient` — download, upload, and streaming support over OkHttp `Request`.
- `TransferResult<T>` — `Progress`, `Success<T>`, `Failure`; transfer-specific aliases: `DownloadResult`, `UploadResult`, `StreamResult`.
- `HttpTransferResponse`, `StreamChunk`, `ProgressRequestBody` — upload/stream/download support types.

**Consumers:** `sample/demo`'s `DemoApiService`/`DemoRemoteDataSourceImpl`.

## `core/di`

Hilt modules for app-wide wiring.

- `AppCoreBindingsModule` — binds `DefaultAppDispatchers` to `AppDispatchers`, `EncryptedSecureStore` to `SecureStore`, `SecureStoreAuthTokenProvider` to `AuthTokenProvider`, `AndroidElapsedRealtimeClock` to `ElapsedRealtimeClock`, and `AndroidStringProvider` to `StringProvider`.
- `AppCoreModule` — provides `SettingsStore` and `LocaleManager`.
- `NetworkBindingsModule` — binds `RetrofitApiClient` and `OkHttpFileTransferClient`; also declares `AuthTokenRefresher` as `@BindsOptionalOf` (absent unless a consuming app binds one — see `core/network/auth`).
- `NetworkModule` — provides `ApiConfig`, `ConnectivityChecker`, `OkHttpClient`, and `Retrofit` (built via `core/network/NetworkClientFactory`). Feature-specific Retrofit services belong in that feature's own DI module.
- `CoroutineScopeModule` — provides the `@ApplicationScope`-qualified, `SupervisorJob() + Dispatchers.Default` `CoroutineScope` used for app-wide fire-and-forget work (startup Initializers, and any future feature's background triggers).

## `core/localization`

Per-app language switching, backed by AndroidX's per-app language API (`AppCompatDelegate.setApplicationLocales`). The manifest declares `android:localeConfig="@xml/locales_config"` and opts into AppCompat `autoStoreLocales`.

- `AppLanguage(languageTag, displayNameResId)` — a **data class**, not an enum, so a consuming app can add languages `:core` ships no strings for. `AppLanguage.ENGLISH`/`VIETNAMESE` are companion constants and `AppLanguage.BUILT_IN` is the list `:core` has display-name strings for; `findByLanguageTag(tag, candidates = BUILT_IN)` resolves a tag.
- `SupportedLanguages(values: List<AppLanguage>)` — `@BindsOptionalOf` in `AppCoreBindingsModule`; bind it to replace `AppLanguage.BUILT_IN`. The `@xml/locales_config` that declares which locales an app actually ships lives in `:app`, not `:core`.
- `AppLocaleApplier` (interface, apply/read locale tags) + `AppCompatLocaleApplier` (real impl) — injected as an interface so `LocaleManager` is unit-testable.
- `LocaleManager(localeApplier = AppCompatLocaleApplier())` — applies a supported `AppLanguage`, clears the override to follow the system, and reports the current app-language override.

**Consumers:** `feature/settings` adapts `LocaleManager` through `SettingsRepository`; `SettingsActivity` renders System/English/Vietnamese in a single-choice dialog and delegates the actual change to its feature-owned `LanguageTransitionAction`, run by core `TransitionActivity`.

## `core/logging`

- `ReleaseTree` (public, extends `timber.log.Timber.Tree`) — filters to WARN+ only, forwards to `android.util.Log`. Planted instead of `Timber.DebugTree()` in non-debuggable builds.

**Consumers:** `TimberInitializer` plants `Timber.DebugTree()` in debug builds and `ReleaseTree` in release builds. Feature code should call `Timber.tag(...).d/i/w/e(...)` instead of `android.util.Log` directly.

## `core/startup`

Formalizes process-startup work via `androidx.startup.Initializer` instead of `Application.onCreate()`.

- `AppStartupEntryPoint` (Hilt `@EntryPoint`) — how Initializers (instantiated by reflection, no constructor injection available) reach `DbPassphraseProvider`, `ThemeManager`, and the `@ApplicationScope CoroutineScope`.
- `TimberInitializer` — plants `Timber.DebugTree()` or `ReleaseTree()` based on the **consuming app's** `ApplicationInfo.FLAG_DEBUGGABLE`, not `:core`'s own `BuildConfig.DEBUG` (which is always `false` in a published AAR and would silence debug logging for every consumer).
- `DbPassphraseWarmupInitializer` — warms `DbPassphraseProvider` on `Dispatchers.IO`. Depends on `TimberInitializer`.
- `ThemeApplyInitializer` — collects `ThemeManager.currentTheme` and applies it reactively. Depends on `TimberInitializer`.
- `LocaleContextInitializer` — captures the process-wide `Context` into `LocaleAppContext` so `AppCompatLocaleApplier.currentLocaleTags()` can read the current per-app locale without an `AppCompatDelegate` needing to be alive yet.

All four are registered as `<meta-data>` entries under `androidx.startup.InitializationProvider` in `AndroidManifest.xml`.

**Consumers:** `AndroidCoreBaseApplication` no longer does any of this directly — see its class doc comment.

## `core/work`

WorkManager wiring: `AndroidCoreBaseApplication` implements `Configuration.Provider`, supplying `HiltWorkerFactory` so `@HiltWorker` classes get constructor injection. WorkManager's default initializer is disabled in `AndroidManifest.xml` (`androidx.work.WorkManagerInitializer` removed from the `androidx.startup.InitializationProvider` merge) so this custom configuration is the one actually used.

- `HeartbeatWorker` (`@HiltWorker`, `CoroutineWorker`) — reference implementation only, not scheduled by default. Copy this shape (constructor pattern, `@Assisted context`/`@Assisted workerParameters`) for real background work.

**Consumers:** none yet — this is infrastructure for the first feature that needs background work.

## `core/ui/text`

- `StringProvider` (interface) — `fun getString(@StringRes resId: Int): String`, lets a ViewModel resolve string resources without holding an Activity/View `Context`.
- `AndroidStringProvider` — the real implementation, backed by an injected `@ApplicationContext Context`. Bound in Hilt via `AppCoreBindingsModule`.
- `UiText` — an immutable resource-or-dynamic UI message, resolved only by a rendering host. Shared presentation states use it instead of requiring a pre-resolved English string.

## `core/ui/base`

Shared UI infrastructure.

- `BaseActivity<VB : ViewBinding>` (abstract) — ViewBinding lifecycle, edge-to-edge inset handling, immersive full-screen display cutout setup, and exit transitions.
- `BaseFragment<VB : ViewBinding>` — Fragment view lifecycle binding and flow collector.
- `BaseDialogFragment<VB : ViewBinding>` — rounded dialog fragment base using `R.drawable.bg_dialog_surface`.
- `BaseBottomSheetDialogFragment<VB : ViewBinding>` — Material bottom-sheet view base.
- `TransitionActivity` — opaque full-screen host for a single `core/ui/transition/TransitionAction`, looked up by a caller-supplied action key from a Hilt `@IntoMap` multibinding; one Activity/manifest entry covers every transition use case instead of a new Activity subclass per case. See `core/ui/transition` below.
- `collectOnStartedBy(lifecycleOwner, action)` (in `LifecycleFlowExtensions.kt`) — shared lifecycle-safe Flow collection; each Base* host's `collectOnStarted` delegates here with its own `LifecycleOwner` (the host itself for `BaseActivity`, `viewLifecycleOwner` for the Fragment/BottomSheet hosts).
- `renderResultState(result, contentRoot, dialogHost, onSuccess)` (in `ResultStateOverlay.kt`) — shared full-screen-loader + `PromptDialogFragment` error rendering; `BaseActivity`/`BaseFragment.bindResultState` both delegate here so the loading/error UI stays identical across hosts.
- `Debouncer` — pure rate limiter with `View.setOnDebouncedClickListener` click rate limiting.
- `ResultRenderState(isLoadingVisible, isContentVisible, isErrorVisible, errorMessage)` — visibility-only projection of a `ResultState<T>`. Not the same mechanism as `ResultStateOverlay`: this one toggles View visibility for screens that render inline (e.g. `sample/designsystem`); `ResultStateOverlay` drives a full-screen loader + dialog for `bindResultState` callers. Pick per-screen based on whether the loading/error UI should be inline or overlay the whole screen.

## `core/ui/components`

- `ButtonStyleDelegate` — shape/ripple background logic, resolving ripple color from `colorControlHighlight`.
- `FrameButton` (`FrameLayout` subclass) — custom shape button implementing `ButtonStyleDelegate`. Enforces 48dp minimum touch target.
- `ShadowLayout` (`FrameLayout` subclass) — rounded shadow layout drawn via elevation outline.
- `ThemedSwitch` (`MaterialSwitch` subclass) — track and thumb tinted from color tokens, text hidden.
- `StyledSnackbar` (object) — shows a Snackbar styled on base colors and returns the Snackbar instance.
- `FullScreenLoaderView` — custom full-screen loading spinner overlay shown during async operations.
- `PromptDialogFragment` — custom status dialog fragment supporting message, technical code, status icon (Success, Error, Info) and primary/secondary action handlers.

## `core/ui/transition`

- `TransitionAction` (`fun interface`, `suspend fun perform(extras: Bundle)`) — a single unit of async work run by `core/ui/base/TransitionActivity` while it covers the screen. Register implementations via a Hilt `@IntoMap` binding keyed by a unique action key.

**Consumers:** `feature/settings`'s `LanguageTransitionAction` runs inside the core `TransitionActivity` for the language-change transition.

## `core/ui/drawable`

- `DrawableShape` (enum: `RECTANGLE`, `OVAL`).
- `ShapeDrawableFactory` (object) — `buildDrawable(...)` programmatically creates GradientDrawables.

## `core/ui/window`

- `Window.setImmersiveMode(enabled)` — edge-to-edge system-bar and display-cutout configuration used by `BaseActivity`.

## `core/ui/theme`

App-wide light/dark/system theme, backed by AppCompat's night mode and persisted through `SettingsStore`.

- `AppTheme` (enum: `LIGHT`, `DARK`, `SYSTEM`, each with a `key: String`) — `AppTheme.fromKey(key)` maps a stored key back to an enum value, defaulting to `SYSTEM` if unrecognized.
- `ThemeManager` (interface) — `currentTheme: Flow<AppTheme>`, `isThemeApplied: StateFlow<Boolean>` (true once the persisted theme has been applied at least once this process), `suspend fun getTheme(): AppTheme`, `suspend fun setTheme(theme: AppTheme)`, `fun applyTheme(theme: AppTheme)`.
- `AndroidThemeManager` — the only implementation; reads/writes `AppSettingsKeys.THEME_MODE` via `SettingsStore` and applies the theme through `AppCompatDelegate.setDefaultNightMode`.
- `ThemeModule` (Hilt `@Module`) — binds `AndroidThemeManager` to `ThemeManager`.

**Consumers:** `feature/settings` adapts `ThemeManager` through `SettingsRepository` for its settings-list state and appearance dialog; `applyTheme` is also called on app start to restore the persisted choice; `MainActivity` reads `isThemeApplied` for its splash screen keep-on-screen condition (Task 3).

## `core/navigation`

- `NavigationOptions` — option model containing custom `TransitionType` (DEFAULT, NONE, SLIDE_HORIZONTAL, FADE).
- `ActivityDestination` — typed activity target model.
- `ActivityNavigator` — navigates using transition override animations (SLIDE_HORIZONTAL, FADE).
- `intentExtra`/`intentExtraNullable`/`fragmentArg`/`fragmentArgNullable` (in `ArgumentDelegates.kt`) — reified, type-safe `ReadOnlyProperty` delegates for Activity `Intent` extras and Fragment arguments, backed by `Bundle.getTyped` (non-deprecated per-type getters, no generic reflection fallback).
- `BundleCompat.copyOf(bundle)` — defensive `Bundle` copy helper.

## `core/time`

- `ElapsedRealtimeClock` — Monotonic clock interface using `SystemClock.elapsedRealtime()` for secure elapsed timing.

---

No other packages exist. Check the source tree before creating new code.

## `:baselineprofile` (separate Gradle module, not `core/`)

A `com.android.test`-type module containing only a Macrobenchmark profile generator — no business/feature code. Exempted from the single-module rule in `CLAUDE.md` because it's closer to `androidTest` than to a feature module.

- `BaselineProfileGenerator` — drives a cold launch + "open demo screen" + back, via `BaselineProfileRule`. Run `./gradlew :app:generateReleaseBaselineProfile` to regenerate `app/src/main/generated/baselineProfiles/baseline-prof.txt` after significant startup-path changes.

**Consumers:** `:app` (via `baselineProfile(project(":baselineprofile"))` and `androidx.profileinstaller:profileinstaller`, which installs the checked-in profile at app install time).

## `src/testFixtures` (published test doubles, not `core/`)

Enabled with `testFixtures { enable = true }`, so both `:core`'s own tests and consuming apps share
one set of doubles instead of re-writing them. Consume with
`testImplementation(testFixtures("com.github.ThanhNg224:AndroidCoreBase:<version>"))`.

- `MainDispatcherRule` — swaps `Dispatchers.Main` for a `TestDispatcher`.
- `FakeSecureStore` — in-memory `SecureStore`; its `stored` map (keyed by `SecureStoreKey.name`) is seedable and assertable.
- `FakeSettingsStore` — in-memory `SettingsStore` that re-emits on change like DataStore does.
- `FakeConnectivityChecker`, `FakeAuthTokenProvider`, `FakeAuthTokenRefresher` (records `callCount`), `FakeAppLocaleApplier` (records `appliedTags`).

Fixtures compile against `:core`'s **public** API only (a `testFixtures` source set is not a friend
module), so anything they need must be public — which is why the contracts above are.

## Public API surface

`:core` is `internal` by default; only what a consuming app needs is public. Public: the
architecture primitives (`UiState`/`UiEvent`/`UiEffect`, `StateViewModel`, `UseCase`,
`AppDispatchers`, `ResultState`/`DomainResult`/`AppError`), `ApiClient`/`ApiConfig`/`ApiResult`,
`AuthSession`/`AuthTokenProvider`/`AuthTokenRefresher`, `ConnectivityChecker`/`NoConnectivityException`,
`FileTransferClient`/`TransferResult`, `SecureStore`/`SettingsStore` and their key types,
`LocaleManager`/`AppLanguage`/`SupportedLanguages`, `ThemeManager`/`AppTheme`, `ReleaseTree`,
`ElapsedRealtimeClock`, `StringProvider`/`UiText`, the `Base*` UI hosts, `TransitionActivity`/`TransitionAction`,
the `intentExtra`/`fragmentArg` delegates, the navigation models, and the `ui/components`,
`ui/drawable`, `ui/window` helpers.

Deliberately `internal`: every Hilt module, the framework-backed implementation behind each public
interface (`EncryptedSecureStore`, `AndroidThemeManager`, `RetrofitApiClient`, `TokenAuthenticator`,
`AndroidConnectivityChecker`, …), the `androidx.startup` initializers, `NetworkClientFactory`, the
Room database/DAO/entity, and `HeartbeatWorker`.

There is currently **no automated binary-compatibility gate**:
`binary-compatibility-validator` registers no `apiDump`/`apiCheck` tasks for a
`com.android.library` module, so it was removed rather than left applied doing nothing. Public API
changes are caught by review plus this internal-by-default discipline — treat any new top-level
declaration as public API unless you mark it `internal`.
