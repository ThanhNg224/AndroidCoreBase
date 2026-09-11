# AndroidCoreBase

[![JitPack](https://jitpack.io/v/ThanhNg224/AndroidCoreBase.svg)](https://jitpack.io/#ThanhNg224/AndroidCoreBase)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-blue.svg)](https://kotlinlang.org)
[![MinSDK](https://img.shields.io/badge/MinSDK-24-green.svg)](https://developer.android.com)
[![TargetSDK](https://img.shields.io/badge/TargetSDK-37-brightgreen.svg)](https://developer.android.com)
[![JDK](https://img.shields.io/badge/JDK-21-orange.svg)](https://www.oracle.com/java)

A production-ready Android base repository using **XML layouts**, **ViewBinding**, **MVVM**, and **Clean Architecture**. `:core` (and the optional `:core:ui-compose`) are published, dependency-injection-agnostic libraries; `:app` is both the reference/sample application and a starter you can clone directly.

Two ways to use this repository — pick one:

- **Clone the starter** — fork/clone the whole repo, rename the package/application ID, delete the `sample/` reference code, and build your app directly on top of `:app` and `:core`. You own the app's Hilt graph already (see `AndroidCoreBaseApplication.kt`, `app/src/main/java/com/example/androidcorebase/di/`).
- **Consume `:core` as a library** — add it (and optionally `:core:ui-compose`) via JitPack into your own existing app, and wire its public factories into your own Hilt module.

See [docs/MIGRATION_V1_TO_V2.md](docs/MIGRATION_V1_TO_V2.md) if you have a v1 consumer to migrate.

---

## Key Features

* **Modular Clean Architecture**: `:core` (and `:core:ui-compose`) are published libraries; `:app` is the consuming application and reference/sample code.
* **Dependency-injection-agnostic library**: `:core` and `:core:ui-compose` apply no Hilt/KSP plugin and ship no DI annotations. Every contract is a public interface with either a public constructor or a factory (`AppDispatchers.default()`, `NetworkClientFactory.createApiClient()`, `ThemeManager.create(settingsStore)`, …). Hilt lives entirely in `:app`, which provides these contracts through its own module — see [Wiring `:core` into your app](#wiring-core-into-your-app) below.
* **JitPack Distribution**: both published modules use `maven-publish`; see [Consuming `:core` via JitPack](#consuming-core-via-jitpack).
* **Encrypted Storage**: Android Keystore-backed `SecureStore` (via `SecureStoreFactory.encrypted(...)`) for secrets, plus `DbPassphraseProvider` for apps that wire up their own SQLCipher database. `:core` ships no database of its own.
* **Self-healing network layer**: Retrofit + OkHttp via `NetworkClientFactory`, with a token-refresh `Authenticator` (single-flight, pluggable `AuthTokenRefresher` your app implements) and file upload/download progress tracking.
* **Per-app locale & dynamic theme**: native per-app language selection (Android 13+ / AppCompat) and zero-flash Light/Dark/System theme management backed by Jetpack DataStore, applied explicitly at app startup by your own `Application` (`:core` plants no process-wide initializer).
* **Passive published manifests**: both `:core` and `:core:ui-compose` publish a manifest with no permissions, components, providers, or services. Your app declares `INTERNET` (and AppCompat's locale auto-storage metadata, if you want it) itself.
* **Screen-owned UI state**: plain `androidx.lifecycle.ViewModel` + `StateFlow`, with transient one-shot requests (snackbars, navigation) modeled as acknowledged state in a FIFO queue, not a `Channel`-backed generic effect type.
* **Reusable test doubles**: `:core` publishes a `testFixtures` artifact (`MainDispatcherRule`, `FakeSecureStore`, `FakeSettingsStore`, `FakeAuthTokenProvider`, `FakeAppLocaleApplier`) so your tests don't hand-roll doubles for its contracts.
* **Strict engineering & quality gates**: Detekt, KtLint, Android Lint (`abortOnError` on every module, including `:app`), Metalava API tracking, and Kover coverage measured over an explicit, positively-selected deterministic (non-UI) surface — see `verifyDeterministicCoreCoverage`.
* **Isolated publication proof**: `scripts/verify-publication.sh` publishes both artifacts to a throwaway repository and builds a standalone consumer project against them, proving the main artifact's POM/manifest carry no Compose or Hilt and no test-only dependency.

---

## Tech Stack & Requirements

| Component | Specification / Technology |
|---|---|
| **Language & JDK** | Kotlin 2.4 / Java 21 |
| **SDK Compatibility** | Min SDK 24 (Android 7.0) / Target SDK 37 |
| **Dependency Injection** | App-owned only: Hilt (Dagger) + KSP in `:app`. `:core`/`:core:ui-compose` have none. |
| **UI Framework** | Material 3, XML ViewBinding, ConstraintLayout, Lottie, Facebook Shimmer; optional Jetpack Compose via `:core:ui-compose` |
| **Network & Serialization** | Retrofit 3, OkHttp 5, Kotlinx Serialization |
| **Local Storage** | Jetpack DataStore Preferences, Android Keystore (no database — declare your own) |
| **Async & Concurrency** | Kotlin Coroutines, StateFlow, WorkManager |
| **Code Quality & Gates** | Detekt, KtLint, Kover, Metalava, Baseline Profiles |

---

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
[docs/FEATURE_TEMPLATE.md](docs/FEATURE_TEMPLATE.md) for a full vertical slice, and
[docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) for the theme, `core_`-prefixed resources and
components (`FrameButton`, `ShadowLayout`, `ThemedSwitch`, `StyledSnackbar`).

> **Note on resources:** every `:core` layout, anim, drawable, raw asset and styleable is
> `core_`-prefixed so your own same-named resources can't silently override them. Styles keep
> `TextAppearance.AndroidCoreBase.*` / `Theme.AndroidCoreBase.*` naming.

---

## Project Architecture

```
AndroidCoreBase/
├── core/                                               # Reusable library module (published to JitPack)
│   └── src/main/java/com/thanhng224/androidcorebase/core/
│       ├── architecture/                               # DefaultAppDispatchers
│       ├── foundation/                                 # Framework-independent contracts: AppDispatchers, SettingsStore, SecureStore
│       ├── localization/                                # AppLanguage, LocaleManager, AppCompatLocaleApplier
│       ├── network/                                     # ApiClient/ApiResult, NetworkClientFactory, auth/, transfer/
│       ├── storage/                                     # settings/ (DataStore), secure/ (Keystore-backed SecureStore, DbPassphraseProvider)
│       └── ui/                                          # BaseActivity/BaseFragment, custom components, delegates, theme
│
├── core/ui-compose/                                     # Optional published Compose interop (AndroidCoreBaseTheme, setThemedContent)
│
├── baselineprofile/                                     # Macrobenchmark module: Baseline Profile generation + startup/frame benchmarks
│
├── integration/consumer/                                # Isolated Gradle project scripts/verify-publication.sh builds against a throwaway repo
│
└── app/                                                 # Application shell, own Hilt graph, and sample/reference code
    └── src/main/java/com/example/androidcorebase/
        ├── AndroidCoreBaseApplication.kt                 # Application entry point; plants logging, launches bounded startup
        ├── MainActivity.kt                               # Single-Activity shell: bottom navigation + NavController
        ├── di/                                           # App-owned Hilt modules wiring :core's public factories
        ├── startup/                                      # AppStartupCoordinator: bounded theme startup, replaces core initializers
        ├── appshell/                                     # App shell destinations (Home)
        ├── feature/                                      # Concrete feature modules (Settings)
        └── sample/                                       # Reference implementations & UI design system
```

---

## Building & Verification Commands

```bash
# Build APKs
./gradlew assembleDebug           # Build Debug APK
./gradlew assembleRelease         # Build Release APK

# Testing & Quality Gates
./gradlew test                    # Run JVM unit tests across all modules
./gradlew check                   # Execute complete quality gate (tests, KtLint, Detekt, Kover, Lint, Metalava)

# Formatting
./gradlew ktlintFormat            # Auto-format Kotlin source code according to project standards
./gradlew detekt                  # Run static code analysis

# Isolated publication proof (see integration/consumer/)
./scripts/verify-publication.sh --quick    # Fast: debug builds of both consumer modes; PR gate
./scripts/verify-publication.sh --release  # Slow: also minified release + R8; main/tag/manual gate
```

---

## Cutting a Release

`:core`'s version comes from `VERSION` (set by JitPack from the tag) falling back to
`VERSION_NAME` in `core/gradle.properties`. To release:

```bash
# 1. update CHANGELOG.md and bump VERSION_NAME in core/gradle.properties to match
# 2. verify the gate and that the published artifacts assemble
./gradlew check :core:assembleRelease :core:ui-compose:assembleRelease
./scripts/verify-publication.sh --release
# 3. tag with the same value and push
git tag v2.0.1 && git push origin v2.0.1
```

JitPack builds the tag using `jitpack.yml` (pinned to JDK 21). Semantic versioning applies to the
published modules' **public** API only — `internal` declarations are not part of the contract.

Test a candidate against a real consumer before tagging:

```bash
./gradlew :core:publishToMavenLocal :core:ui-compose:publishToMavenLocal
# then add mavenLocal() in the consumer project, or use scripts/verify-publication.sh directly
```

---

## Engineering Documentation

For detailed guidelines and architectural specifications, refer to the `docs/` folder:

- [CORE_V2_DESIGN.md](docs/CORE_V2_DESIGN.md) — The v2 architecture decision record: what changed, why, and the full public/internal exposure table.
- [MIGRATION_V1_TO_V2.md](docs/MIGRATION_V1_TO_V2.md) — Every removed v1 API and its exact v2 replacement.
- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — Layering, state management, and dependency rules.
- [CORE_MODULES.md](docs/CORE_MODULES.md) — Structure and encapsulation rules for `:core`.
- [FEATURE_TEMPLATE.md](docs/FEATURE_TEMPLATE.md) — Worked example of a full feature slice.
- [DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) — Theme, tokens, and component catalogue.
- [STANDARD.md](docs/STANDARD.md) — Coding conventions, naming, and formatting rules.
- [GIT_FLOW.md](docs/GIT_FLOW.md) — Branching strategy, commit conventions, and PR workflow.
- [MODERNIZATION.md](docs/MODERNIZATION.md) — Historical v1 hardening record; superseded by `CORE_V2_DESIGN.md` for anything it contradicts.
- [CHANGELOG.md](CHANGELOG.md) — Released versions and breaking changes.

Licensed under the [MIT License](LICENSE).
