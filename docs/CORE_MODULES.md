# CORE_MODULES.md

`core/` lives in the `:core` Gradle module under `com.thanhng224.androidcorebase.core`; `:app` consumes it through `implementation(project(":core"))`. `:core:ui-compose` (package `com.thanhng224.androidcorebase.core.compose`) is a second, optional published module for Jetpack Compose interop, depending on `:core`.

One section per package that actually exists in this codebase today. Each section lists the real public API surface and which feature(s) currently consume it. If a class/file isn't listed here, it doesn't exist yet — don't assume it does. See `docs/CORE_V2_DESIGN.md` for the full v2 architecture decision record and `docs/MIGRATION_V1_TO_V2.md` for what changed from v1.

**Neither published module applies a DI framework.** `:core` and `:core:ui-compose` run in Kotlin
**explicit API mode**, and every public contract has a public constructor or a small factory object
next to it — there is no Hilt binding to look for. `:app` owns the only Hilt graph in the repo and
wires these factories in `app/src/main/java/com/example/androidcorebase/di/`. An implementation
class stays `internal` behind its public interface/factory (`EncryptedFileSecureStore`,
`AndroidThemeManager`, `RetrofitApiClient`, `TokenAuthenticator`, …) exactly as before — only *how*
you obtain the public-facing instance changed, from `@Inject` to a direct call.

## `core/foundation`

Framework-independent contracts. A build-logic task (`verifyFrameworkIndependentSources`) fails the
build if anything here imports `android.*`, `androidx.*`, Retrofit, OkHttp, Hilt, Material, Compose,
or the core `R` class — so these types are pure Kotlin/coroutines and trivially unit-testable.

- `AppDispatchers` (interface: `main`/`io`/`default` `CoroutineDispatcher`s) — `AppDispatchers.default()` companion factory constructs the real implementation (`core/architecture/DefaultAppDispatchers`, internal).
- `SettingsKey<T>` (sealed class, `name`/`defaultValue`) with 5 typed subclasses: `StringKey`, `IntKey`, `LongKey`, `BooleanKey`, `FloatKey`.
- `SettingsStore` (interface) — `fun <T> observe(key): Flow<T>`, `suspend fun <T> get(key): T`, `suspend fun <T> set(key, value)`, `suspend fun <T> remove(key)`.
- `SecureStoreKey`, `SecureStore` (interface: `getString`/`putString`/`remove`/`clear`), `SecureStoreKeys` (built-in keys: `AUTH_TOKEN`, `REFRESH_TOKEN`).

## `core/architecture`

- `DefaultAppDispatchers` (internal) — the only `AppDispatchers` implementation, constructed via `AppDispatchers.default()`.

## `core/storage/settings`

A typed, testable settings store backed by Jetpack DataStore (`androidx.datastore:datastore-preferences`).

- `SettingsStoreFactory.create(dataStore: DataStore<Preferences>): SettingsStore` — the public factory. Android-typed (`DataStore<Preferences>`), so it lives beside its implementation rather than as a companion on the framework-independent `SettingsStore` contract.
- `DataStoreSettingsStore` (internal) — the implementation. `observe` catches an upstream `IOException` and emits `emptyPreferences()` (typed defaults apply) rather than propagating it; any other failure rethrows.
- `AppSettingsKeys` — `THEME_MODE` (String, default `"system"`), `FIRST_OPEN_AT` (Long, default `0L`), `OPEN_COUNT` (Int, default `0`), `DEBUG_LOGGING_ENABLED` (Boolean, default `false`), `LANGUAGE_TAG` (String, default `""`; empty means "follow system").

You supply your own `DataStore<Preferences>` (e.g. via `androidx.datastore.preferences.preferencesDataStore` in your own `di/` package) — `:core` no longer owns a `Context.appSettingsDataStore` delegate.

## `core/storage/secure`

- `SecureStoreFactory.encrypted(context, dispatchers): SecureStore` — the public factory.
- `EncryptedFileSecureStore` (internal) — the implementation. A single small file under `Context.noBackupFilesDir` (never in Auto Backup), updated atomically via `androidx.core.util.AtomicFile` so a failed write preserves the previous complete value. The whole payload is AES-256-GCM-encrypted with a non-exportable Android Keystore key and a fresh random IV on every write (`EncryptedFileCodec`, internal, owns the envelope format).
- `DbPassphraseProvider(secureStore: SecureStore)` (public constructor) — `suspend fun getOrCreate(): ByteArray` returns a stable 32-byte random passphrase, Base64-persisted through `SecureStore` and memoized in memory. For a consumer's *own* SQLCipher `SupportFactory`; `:core` itself has no database. See README.md's "Optional: your own encrypted database".

**Consumers:** `DemoRepositoryImpl` (sample-private counter key + `SettingsStore`); `SettingsRepositoryImpl` persists the chosen language and theme through `SettingsStore`; `SecureStoreAuthTokenProvider` reads `SecureStoreKeys.AUTH_TOKEN`.

`:core` ships **no database**. Room's `@Database` fixes its `entities` list at compile time in the annotated class, so a library cannot hand a consumer one to extend, and SQLCipher's native library costs every consuming app real size R8 cannot strip. Declare your own `@Database` and add Room + SQLCipher to your own module if you need one.

## `core/network`

- `ApiResult<out T>` (sealed interface) — `Success<T>(data)`, `Failure(error: ApiFailure)`.
- `ApiFailure` (sealed interface) — `Http(code, message)`, `Network(cause)`, `Serialization(cause)`, `EmptyBody`.
- `ApiConfig(baseUrl, enableLogging = false, connectTimeoutSeconds/readTimeoutSeconds/writeTimeoutSeconds = 30)` — **supplied by the consuming app**, not by `:core`. `:core` ships no base URL on purpose; `:app` provides its own in `app/.../di/AppNetworkModule.kt`.
- `ApiClient` (interface) — `suspend fun <T> execute(call: suspend () -> retrofit2.Response<T>): ApiResult<T>`.
- `RetrofitApiClient` (internal) — the `ApiClient` implementation; classifies success/HTTP error/empty body, catches `IOException` as `Network`, any other `Exception` as `Serialization`, and always rethrows `CancellationException` before those catches. Empty-body detection uses HTTP 204/205 status (RFC 9110), not a null-body check — OkHttp 5's `Response.body` is non-nullable.
- `NetworkClientFactory` (public object) — the one subsystem factory for OkHttp/Retrofit/auth/transfer construction:
  - `createOkHttpClient(config, interceptors = emptyList(), authenticator = Authenticator.NONE): OkHttpClient`
  - `createRetrofit(config, okHttpClient): Retrofit`
  - `createApiClient(): ApiClient`
  - `createFileTransferClient(okHttpClient, dispatchers): FileTransferClient`
  - `createAuthTokenProvider(authSession): AuthTokenProvider`
  - `createAuthenticator(authSession, tokenRefresher: (() -> AuthTokenRefresher)? = null): Authenticator`

### `core/network/auth`

- `AuthSession(secureStore: SecureStore)` (public constructor) — the gateway a consuming app uses to read/write `SecureStoreKeys.AUTH_TOKEN`/`REFRESH_TOKEN`: `peekAccessToken()`, `getAccessToken()`, `getRefreshToken()`, `setTokens(accessToken, refreshToken?)`, `clear()`.
- `AuthTokenProvider` (interface: `peekToken()`/`suspend fun getToken()`) + `NoOpAuthTokenProvider`. `SecureStoreAuthTokenProvider` (internal) is the real implementation, reachable only via `NetworkClientFactory.createAuthTokenProvider(authSession)`.
- `AuthTokenInterceptor(authTokenProvider)` (public) — adds the token from `peekToken()` (falling back to a one-time blocking `getToken()`) into the `Authorization` header if present.
- `AuthTokenRefresher` (public interface, `suspend fun refresh(refreshToken: String?): String?`) — extension point a consuming app implements and passes as the `tokenRefresher` lambda to `NetworkClientFactory.createAuthenticator`. `:core` ships no implementation; without one, the authenticator gives up on a 401 instead of pretending to refresh.
- `TokenAuthenticator` (internal, `okhttp3.Authenticator`) — on a 401, mutex-guards a single in-flight refresh per process: if `AuthSession`'s cached token already differs from the one that just failed (another caller already refreshed), reuses it; otherwise invokes the `tokenRefresher` lambda and persists the result via `AuthSession.setTokens`. Allows at most **one** retry (`response.priorResponse != null` short-circuits), not two.

### `core/network/transfer`

- `FileTransferClient` (interface: `download`/`upload`/`stream`) + `OkHttpFileTransferClient` (internal), reachable via `NetworkClientFactory.createFileTransferClient`.
- `TransferEvent<P, R>` (sealed interface) — `Progress`, `Completed<R>`, `Failed(error: TransferError)`, and (stream only) `Payload<P>` for a raw chunk. Type aliases: `DownloadEvent`, `UploadEvent`, `StreamEvent`.
- `TransferError` (sealed interface) — `Http(code)`, `Network(cause)`, `FileSystem(cause)`, `EmptyBody`.
- `HttpTransferMetadata(code, headers)` — upload completion metadata.
- Downloads write through `androidx.core.util.AtomicFile` (a failed write never corrupts a previously-completed file). `download`/`upload` `conflate()` their progress events (safe because neither ever emits a `Payload`); `stream` does not conflate, since a dropped `Payload` chunk would silently corrupt the stream.

**Consumers:** `sample/demo`'s `DemoApiService`/`DemoRemoteDataSourceImpl` use `ApiClient` only; nothing in `:app` currently exercises `FileTransferClient` or the auth token-refresh path — they are base infrastructure any consumer can wire when needed.

## `core/localization`

Per-app language switching, backed by AndroidX's per-app language API (`AppCompatDelegate.setApplicationLocales`).

- `AppLanguage(languageTag, displayNameResId)` — a **data class**, not an enum, so a consuming app can add languages `:core` ships no strings for. `AppLanguage.ENGLISH`/`VIETNAMESE` are companion constants and `AppLanguage.BUILT_IN` is the list `:core` has display-name strings for; `findByLanguageTag(tag, candidates = BUILT_IN)` resolves a tag.
- `SupportedLanguages(values: List<AppLanguage>)` — pass your own list directly to `LocaleManager`'s constructor to replace `AppLanguage.BUILT_IN`.
- `AppLocaleApplier` (interface, apply/read locale tags) + `AppCompatLocaleApplier(context)` (public constructor) — injected as an interface so `LocaleManager` is unit-testable.
- `LocaleManager(localeApplier, supportedLanguages = AppLanguage.BUILT_IN)` (public constructor) — applies a supported `AppLanguage`, clears the override to follow the system, and reports the current app-language override by reading `localeApplier.currentLocaleTags()`.

**Consumers:** `feature/settings` persists the chosen language through `SettingsStore` first, then applies it via `LocaleManager` — see "Settings and Locale Mutation" in `docs/CORE_V2_DESIGN.md`. `SettingsFragment` renders System/English/Vietnamese in a single-choice dialog.

## `core/ui/base`

The Activity/Fragment/Dialog base classes. `:core` no longer publishes a generic result-rendering
mechanism — a screen renders its own explicit sealed state (see `docs/ARCHITECTURE.md`'s "UI State").

- `BaseActivity` (abstract) — the neutral base. Calls `enableEdgeToEdge()` before `setContentView`, applies `useImmersiveMode`, and offers `collectOnStarted` (lifecycle-safe Flow collection). Two overridable flags: `useImmersiveMode` (default `false`) and `applyInsetsToRoot` (default `true`; see `core/ui/window`).
- `BaseBindingActivity<VB : ViewBinding>` — XML path. Inflates `VB`, sets it as content, applies system-bar insets to `binding.root` unless opted out, then calls `onBindingReady`. Nulls the binding in `onDestroy`.
- `BaseFragment<VB>` — also offers `collectOnStarted`.
- `BaseDialogFragment<VB>` — adds overridable `dialogAnimation` (`DialogAnimation`: `SLIDE`, `SCALE`, `FADE`, `NONE`) and `backgroundDrawableRes`, and clamps dialog width in `onStart` using `core_dialog_screen_margin`/`core_dialog_max_width`. `Animation_AndroidCoreBase_Dialog_Fade` (for `DialogAnimation.FADE`) is a permanent themes.xml style — its two anim resources are never swept even when unrelated features that also referenced them are removed.
- `BaseBottomSheetDialogFragment<VB>`.

**Misc:**
- `Debouncer(intervalMs = 600L)` with `shouldAllow(nowMs)`, and `View.setOnDebouncedClickListener(intervalMs, action)` — the extension is the normal entry point.
- `Flow<T>.collectOnStartedBy(lifecycleOwner, action)` — the shared implementation behind every base class's `collectOnStarted`. Each host passes its own `LifecycleOwner`: the Activity itself for `BaseActivity`, `viewLifecycleOwner` for the Fragment/BottomSheet hosts.

**Consumers:** `BaseBindingActivity` — `MainActivity`. `BaseFragment` — `HomeFragment`, `SettingsFragment`, `DemoFragment`, `DesignSystemFragment`. `setOnDebouncedClickListener` — `DemoFragment`. **No consumer yet:** `BaseDialogFragment`, `BaseBottomSheetDialogFragment` (used internally, not directly by `:app`).

## `core/ui/components`

- `ButtonStyleDelegate` — shape/ripple background logic, resolving ripple color from `colorControlHighlight`.
- `FrameButton` (`FrameLayout` subclass) — custom shape button implementing `ButtonStyleDelegate`. Enforces 48dp minimum touch target.
- `ShadowLayout` (`FrameLayout` subclass) — rounded shadow layout drawn via elevation outline.
- `ThemedSwitch` (`MaterialSwitch` subclass) — track and thumb tinted from color tokens, text hidden.
- `StyledSnackbar` (object) — shows a Snackbar styled on base colors and returns the Snackbar instance.

## `core/ui/drawable`

- `DrawableShape` (enum: `RECTANGLE`, `OVAL`).
- `ShapeDrawableFactory` (object) — `buildDrawable(...)` programmatically creates GradientDrawables.

## `core/ui/window`

- `Window.setImmersiveMode(enabled)` — edge-to-edge system-bar and display-cutout configuration used by `BaseActivity`.

## `core/ui/text`

- `UiText` (sealed interface: `DynamicString(value)`, `StringResource(resId, vararg formatArgs)`) — an immutable resource-or-dynamic UI message. `UiText.resolve(context): String` is the only Android-touching part, kept as a separate extension so `UiText` itself stays a plain data type.

## `core/ui/theme`

App-wide light/dark/system theme, backed by AppCompat's night mode and persisted through `SettingsStore`.

- `AppTheme` (enum: `LIGHT`, `DARK`, `SYSTEM`, each with a `key: String`) — `AppTheme.fromKey(key)` maps a stored key back to an enum value, defaulting to `SYSTEM` if unrecognized.
- `ThemeManager` (interface) — `currentTheme: Flow<AppTheme>`, `isThemeApplied: StateFlow<Boolean>` (true once the persisted theme has been applied at least once this process), `suspend fun getTheme(): AppTheme`, `suspend fun setTheme(theme: AppTheme)` (persists then applies), `fun applyTheme(theme: AppTheme)`. `ThemeManager.create(settingsStore)` companion factory constructs the real implementation (`AndroidThemeManager`, internal).

**Consumers:** `feature/settings` adapts `ThemeManager` through `SettingsRepository` for its settings-list state and appearance dialog. `app/startup/AppStartupCoordinator` calls `applyTheme` once at process start with a 2-second bound (falling back to `AppTheme.SYSTEM` on an `IOException`), replacing the deleted `androidx.startup` initializers. `MainActivity` reads `isThemeApplied` for its splash screen keep-on-screen condition.

## `core/navigation`

- `intentExtra`/`intentExtraNullable`/`fragmentArg`/`fragmentArgNullable` (in `ArgumentDelegatesKt`) — reified, type-safe `ReadOnlyProperty` delegates for Activity `Intent` extras and Fragment arguments, backed by `Bundle.getTyped` (non-deprecated per-type getters, no generic reflection fallback).
- `BundleCompat.copyOf(bundle)` — defensive `Bundle` copy helper.

`ActivityNavigator`/`ActivityDestination`/`NavigationOptions`/`TransitionType` were removed: their
only consumer was the old `SettingsActivity`, which the single-Activity/Fragment navigation redesign
replaced (see `docs/MIGRATION_V1_TO_V2.md`).

---

## `:core:ui-compose` (separate published module)

Optional Compose interop, published as `AndroidCoreBase-ui-compose`. `api(project(":core"))`; depends
on the Compose BOM, `androidx.compose.ui`, `androidx.compose.material3`.

- `AndroidCoreBaseTheme` (`@Composable` function) — wraps content in a Compose `MaterialTheme` whose `ColorScheme` is read from the same `core_color_*` resources `:core`'s XML theme uses, so both stay in sync from one edit.
- `ComposeView.setThemedContent(content)` — the interop entry point for embedding a themed `ComposeView` in an XML layout, disposing on `ViewTreeLifecycleOwner` destruction.
- `BaseComposeActivity` (abstract) — wraps an abstract `@Composable Content()` in `AndroidCoreBaseTheme` via `setContent`. Has no `binding.root` to pad, so a Compose screen applies insets itself inside `Content()`.

**Any module that declares or calls `@Composable` code — including a consuming app writing its own
composables — must apply `org.jetbrains.kotlin.plugin.compose` itself.** The Compose compiler
transforms `@Composable` lambda parameters at the bytecode level per module; a module without the
plugin produces a call site that compiles but throws `NoSuchMethodError` at runtime.

**Consumer:** `sample/designsystem`'s `DesignSystemFragment` embeds a themed `ComposeView`.

## `:baselineprofile` (separate Gradle module, not `core/`)

A `com.android.test`-type module containing only Macrobenchmark tests — no business/feature code.
Exempted from the single-module rule in `CLAUDE.md` because it's closer to `androidTest` than to a
feature module.

- `CriticalJourney.execute(device, packageName)` — the one shared journey both files below exercise: Home → Demo (wait weather, increment) → UI Kit (wait Compose showcase) → Settings (change theme) → Home.
- `BaselineProfileGenerator` — drives `CriticalJourney` via `BaselineProfileRule`. Run `./gradlew :app:generateBaselineProfile` on an authorized device/emulator to regenerate `app/src/main/baseline-prof.txt`; copy only the plugin-produced profile, never hand-write rules.
- `StartupBenchmark` — measures cold-start `StartupTimingMetric`/`FrameTimingMetric` over `CriticalJourney` under `CompilationMode.None()` vs. `CompilationMode.Partial(BaselineProfileMode.Require)`, 10 iterations each. Run via `./gradlew :baselineprofile:connectedCheck`. Only runs in CI on manual dispatch or the weekly schedule (see `.github/workflows/check.yml`), never on a normal pull request.

**Consumers:** `:app` (via `baselineProfile(project(":baselineprofile"))` and `androidx.profileinstaller:profileinstaller`, which installs the checked-in profile at app install time).

## `integration/consumer` (separate, isolated Gradle build, not part of this repo's own build)

Not a `:core` module and not included in the root `settings.gradle.kts`. A standalone Gradle project
`scripts/verify-publication.sh` builds twice (`-PincludeCompose=false` and `=true`) against a
throwaway Maven repository the script publishes `:core`/`:core:ui-compose` to, proving both artifacts
resolve and build (including minified `--release` R8) with nothing but that repository plus
Google/Maven Central — no `mavenLocal()`, no project substitution.

## `src/testFixtures` (published test doubles, not `core/`)

Enabled with `testFixtures { enable = true }`, so both `:core`'s own tests and consuming apps share
one set of doubles instead of re-writing them. Consume with
`testImplementation(testFixtures("com.github.ThanhNg224:AndroidCoreBase:<version>"))`.

- `MainDispatcherRule` — swaps `Dispatchers.Main` for a `TestDispatcher`.
- `FakeSecureStore` — in-memory `SecureStore`.
- `FakeSettingsStore` — in-memory `SettingsStore` that re-emits on change like DataStore does.
- `FakeAuthTokenProvider`, `FakeAppLocaleApplier` (records `appliedTags`).

Fixtures compile against `:core`'s **public** API only (a `testFixtures` source set is not a friend
module), so anything they need must be public — which is why the contracts above are. `junit` and
`kotlinx-coroutines-test` are `testFixturesImplementation`, not `testFixturesApi`: they never leak
into the main published POM (see "Published Consumer Verification" in `docs/CORE_V2_DESIGN.md`).

## Public API surface

`:core` and `:core:ui-compose` are `internal` by default; only what a consuming app needs is public.
See `docs/CORE_V2_DESIGN.md`'s "Dependency Injection Contract" table for the exact
public-factory/internal-implementation pairing per capability. Public, in short: `AppDispatchers` +
`.default()`, `SettingsKey`/`SettingsStore`/`SettingsStoreFactory`, `SecureStore`/`SecureStoreKey`/
`SecureStoreFactory`, `ApiClient`/`ApiResult`/`ApiConfig`/`NetworkClientFactory`,
`FileTransferClient`/`TransferEvent`/`TransferError`, `AuthSession`/`AuthTokenProvider`/
`AuthTokenInterceptor`/`AuthTokenRefresher`, `DbPassphraseProvider`, `LocaleManager`/`AppLanguage`/
`AppCompatLocaleApplier`, `ThemeManager` + `.create(...)`/`AppTheme`, `UiText`, the `Base*` UI hosts,
the `intentExtra`/`fragmentArg` delegates, `ui/components`/`ui/drawable`/`ui/window`, and (in
`:core:ui-compose`) `AndroidCoreBaseTheme`/`setThemedContent`/`BaseComposeActivity`.

Deliberately `internal`: `DefaultAppDispatchers`, `DataStoreSettingsStore`, `EncryptedFileSecureStore`/
`EncryptedFileCodec`, `RetrofitApiClient`, `OkHttpFileTransferClient`, `SecureStoreAuthTokenProvider`,
`TokenAuthenticator`, `AndroidThemeManager` — every framework-backed implementation behind a public
interface/factory.

Metalava (`apiDump`/`apiCheck`, gated in `check`) tracks both modules' public API against a committed
`core/api/core.api` / `core/ui-compose/api/ui-compose.api` signature file — the automated
binary-compatibility gate the pre-v2 base was missing.

## Consuming `:core` via JitPack

### 1. Add the JitPack repository

In your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

### 2. Add the library dependency

In your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.ThanhNg224.AndroidCoreBase:AndroidCoreBase:v2.0.1")

    // Optional: only if you write Compose screens against AndroidCoreBaseTheme/setThemedContent.
    implementation("com.github.ThanhNg224.AndroidCoreBase:AndroidCoreBase-ui-compose:v2.0.1")

    testImplementation(testFixtures("com.github.ThanhNg224.AndroidCoreBase:AndroidCoreBase:v2.0.1"))
}
```

> **The group is `com.github.ThanhNg224.AndroidCoreBase`, with the repository name appended.** Once
> a JitPack build publishes more than one module it namespaces every module under
> `com.github.<user>.<repo>` and turns the short `com.github.ThanhNg224:AndroidCoreBase` coordinate
> into an aggregator POM that depends on **both** modules. Using the short coordinate therefore
> drags Compose into an XML-only app — exactly what splitting `:core:ui-compose` out was meant to
> prevent. `com.github.ThanhNg224:AndroidCoreBase-ui-compose` does not exist at all (HTTP 401).
>
> `v2.0.0` is **withdrawn** (tag deleted, superseded by `v2.0.1`), but its JitPack build is cached
> permanently and still resolves. It predates the second module, so there the short coordinate
> *was* the real AAR — anyone who picked it up during its short life must change the group, not
> just the version.

Check available tags and builds on [JitPack: ThanhNg224/AndroidCoreBase](https://jitpack.io/#ThanhNg224/AndroidCoreBase).

### 3. Configure your own module

`:core` is a plain Android library with no DI framework, so a consuming module needs only:

```kotlin
plugins {
    id("com.android.application")   // AGP 9's built-in Kotlin support covers Kotlin sources too
}

android {
    defaultConfig { minSdk = 24 }   // :core's minSdk
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { viewBinding = true }   // needed for BaseActivity/BaseFragment
}
```

Retrofit, OkHttp, coroutines, AppCompat, Fragment, lifecycle-viewmodel, Material, and DataStore
arrive transitively as `api` dependencies — you do not need to redeclare them to use `:core`'s API.
Add your own DI framework (Hilt, Koin, or manual construction) on top; nothing in `:core` requires
one.

`INTERNET` is not declared by `:core`'s manifest. Add it yourself if you use the network APIs:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Wiring `:core` into your app

`:core` and `:core:ui-compose` expose every contract as a public interface plus either a public
constructor or a small factory object. Nothing in this section is Hilt-specific — the same calls
work with any DI framework, or with no framework at all. `app/src/main/java/com/example/androidcorebase/di/`
is a complete, working example of this wiring (`AppCoreModule.kt`, `AppNetworkModule.kt`) if you'd
rather read real code than this table.

| Capability | How to construct it |
|---|---|
| Coroutine dispatchers | `AppDispatchers.default()` |
| Preferences storage | `SettingsStoreFactory.create(dataStore: DataStore<Preferences>)` |
| Secure storage | `SecureStoreFactory.encrypted(context, dispatchers)` |
| Theme | `ThemeManager.create(settingsStore)`, then call `applyTheme(...)` once at startup |
| Locale | `LocaleManager(localeApplier = AppCompatLocaleApplier(context))` |
| API execution | `NetworkClientFactory.createApiClient()` |
| OkHttp/Retrofit | `NetworkClientFactory.createOkHttpClient(config, interceptors, authenticator)` / `createRetrofit(config, okHttpClient)` |
| File transfer | `NetworkClientFactory.createFileTransferClient(okHttpClient, dispatchers)` |
| Auth session | `AuthSession(secureStore)` |
| Token provider / authenticator | `NetworkClientFactory.createAuthTokenProvider(authSession)` / `createAuthenticator(authSession, tokenRefresher)` |
| DB passphrase | `DbPassphraseProvider(secureStore)` |

A minimal Hilt module wiring the pieces a typical app needs:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppCoreModule {
    @Provides
    @Singleton
    fun provideAppDispatchers(): AppDispatchers = AppDispatchers.default()

    @Provides
    @Singleton
    fun provideSettingsStore(@ApplicationContext context: Context): SettingsStore =
        SettingsStoreFactory.create(context.appSettingsDataStore) // your own DataStore<Preferences> delegate

    @Provides
    @Singleton
    fun provideSecureStore(@ApplicationContext context: Context, dispatchers: AppDispatchers): SecureStore =
        SecureStoreFactory.encrypted(context, dispatchers)

    @Provides
    @Singleton
    fun provideThemeManager(settingsStore: SettingsStore): ThemeManager = ThemeManager.create(settingsStore)
}
```

### If you write Compose screens

Depend on `AndroidCoreBase-ui-compose` and apply `org.jetbrains.kotlin.plugin.compose` (matching
your Kotlin version) in **any module** that declares or calls `@Composable` code against
`AndroidCoreBaseTheme`/`ComposeView.setThemedContent` — not only where you first add it. The Compose
compiler transforms `@Composable` lambda parameters at the bytecode level per module; a module
missing the plugin produces a call site that compiles cleanly but throws `NoSuchMethodError` at
runtime.

### Optional: your own encrypted database

`:core` ships **no** database — Room's `@Database` fixes its `entities` list at compile time in the
annotated class, so a library cannot hand you one to extend. Add Room + SQLCipher in your own module
and declare your own `@Database`. What `:core` does give you is `DbPassphraseProvider`: a stable
random passphrase, generated once and persisted behind the Keystore via `SecureStore`.

```kotlin
@Provides
@Singleton
fun provideDatabase(
    @ApplicationContext context: Context,
    passphraseProvider: DbPassphraseProvider,
): MyDatabase {
    val passphrase = runBlocking { passphraseProvider.getOrCreate() }
    return Room.databaseBuilder(context, MyDatabase::class.java, "my_database.db")
        .openHelperFactory(SupportOpenHelperFactory(passphrase.toByteArray()))
        .build()
}
```

`getOrCreate()` is `suspend` because the first call reads encrypted storage from disk, while a Hilt
`@Provides` boundary is synchronous — hence the `runBlocking`. That is tolerable because it happens
once and Room builds lazily on first query. To keep it off the critical path entirely, warm it from
your own `Application.onCreate()` on a background dispatcher so the `@Provides` call hits the
memoized value.

### Required: supply an `ApiConfig`

`:core` deliberately ships **no** base URL. `NetworkClientFactory.createOkHttpClient`/`createRetrofit`
both take an `ApiConfig` you construct yourself:

```kotlin
@Provides
@Singleton
fun provideApiConfig() =
    ApiConfig(
        baseUrl = BuildConfig.API_BASE_URL,
        enableLogging = BuildConfig.DEBUG,
        readTimeoutSeconds = 20, // per-timeout overrides are optional
    )
```

### Optional: enable real token refresh

`NetworkClientFactory.createAuthenticator(authSession, tokenRefresher)` retries a 401 once with a
fresh token, but only if you pass a `tokenRefresher` lambda — refreshing needs an API contract
`:core` can't know. Concurrent 401s share a single refresh, and the result is persisted through
`AuthSession`.

```kotlin
class MyTokenRefresher @Inject constructor(
    private val api: AuthApi, // built on a plain client, NOT the :core one, or you recurse into this same auth flow
) : AuthTokenRefresher {
    override suspend fun refresh(refreshToken: String?): String? =
        refreshToken?.let { runCatching { api.refresh(it).accessToken }.getOrNull() }
}

@Provides
@Singleton
fun provideAuthenticator(authSession: AuthSession, refresher: MyTokenRefresher): Authenticator =
    NetworkClientFactory.createAuthenticator(authSession) { refresher }
```

Read and write the tokens themselves through the injectable `AuthSession`
(`getAccessToken()`, `setTokens(...)`, `clear()` on logout).

### Optional: add languages

`AppLanguage` is a data class, not a closed enum, so you can ship locales `:core` has no strings for.
Declare them in your own `@xml/locales_config` and pass the list directly:

```kotlin
LocaleManager(
    localeApplier = AppCompatLocaleApplier(context),
    supportedLanguages = AppLanguage.BUILT_IN + AppLanguage("ja", R.string.language_japanese),
)
```

### Screens

```kotlin
@AndroidEntryPoint
class ProfileActivity : BaseBindingActivity<ActivityProfileBinding>() {
    private val viewModel: ProfileViewModel by viewModels()
    private val userId: String by intentExtra(EXTRA_USER_ID) // type-safe extras

    override fun inflateBinding(inflater: LayoutInflater) = ActivityProfileBinding.inflate(inflater)

    override fun onBindingReady(savedInstanceState: Bundle?) {
        viewModel.state.collectOnStarted(::render) // lifecycle-aware, STARTED
    }
}
```

ViewModels are plain `androidx.lifecycle.ViewModel` with a `MutableStateFlow`/`StateFlow` and named
intent functions (`onEvent`, or direct methods like `selectTheme`). A transient one-shot request
(snackbar, navigation) is modeled as a small `pendingMessages: List<PendingMessage>` field on the
state itself — acknowledged (`onMessageHandled(id)`) once shown — not a `Channel`-backed effect
type, so it survives configuration changes without a lost or duplicated emission. See
[docs/FEATURE_TEMPLATE.md](FEATURE_TEMPLATE.md) for a full vertical slice, and
[docs/DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) for the theme, `core_`-prefixed resources and
components (`FrameButton`, `ShadowLayout`, `ThemedSwitch`, `StyledSnackbar`).

> **Note on resources:** every `:core` layout, anim, drawable, raw asset and styleable is
> `core_`-prefixed so your own same-named resources can't silently override them. Styles keep
> `TextAppearance.AndroidCoreBase.*` / `Theme.AndroidCoreBase.*` naming.

---
