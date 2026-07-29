# AndroidXmlBase

[![JitPack](https://jitpack.io/v/ThanhNg224/AndroidXmlBase.svg)](https://jitpack.io/#ThanhNg224/AndroidXmlBase)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)](https://kotlinlang.org)
[![MinSDK](https://img.shields.io/badge/MinSDK-24-green.svg)](https://developer.android.com)
[![TargetSDK](https://img.shields.io/badge/TargetSDK-37-brightgreen.svg)](https://developer.android.com)
[![JDK](https://img.shields.io/badge/JDK-21-orange.svg)](https://www.oracle.com/java)

A production-ready, state-of-the-art Android base repository utilizing **XML layouts**, **ViewBinding**, **MVVM**, and **Clean Architecture**. Built with a modular structure separating reusable foundation (`:core`) from application/sample code (`:app`), optimized for speed, security, maintainability, and enterprise-grade scalability.

---

## 🚀 Key Features

* **Modular Clean Architecture**: Strict separation of concerns between `:core` (published library) and `:app` (consuming application & reference samples).
* **JitPack Distribution**: `:core` is configured with `maven-publish` for easy integration into any Android project via JitPack.
* **Encrypted Storage & Database**: Secure local persistence powered by **Room + SQLCipher** (with runtime Keystore passphrase generation) and Android KeyStore-backed **`EncryptedSecureStore`**.
* **Self-Healing Network Layer**: **Retrofit + OkHttp** client with a token-refresh `Authenticator` (single-flight, pluggable `AuthTokenRefresher` your app implements against its own refresh endpoint), file upload/download progress tracking, and offline status interceptors.
* **Per-App Locale & Dynamic Theme System**: Native per-app language selection (Android 13+ / Jetpack Compat) and zero-flash Light/Dark/System theme management backed by **Jetpack DataStore**.
* **Encapsulated UI Toolkit**: Type-safe property delegates (`intentExtra`, `fragmentArg`), result state overlay renderers, custom Material 3 components, and smooth Lottie/Shimmer loading states.
* **Reusable Test Doubles**: `:core` publishes a `testFixtures` artifact (`MainDispatcherRule`, `FakeSecureStore`, `FakeSettingsStore`, `FakeConnectivityChecker`, …) so your tests don't hand-roll doubles for its contracts.
* **Strict Engineering & Quality Gates**: **Detekt**, **KtLint**, Android **Lint** (`abortOnError`), and **Kover** line-coverage verification measured over the whole `:core` module — currently ~82%, gated at 80%.

---

## 🛠️ Tech Stack & Requirements

| Component | Specification / Technology |
|---|---|
| **Language & JDK** | Kotlin 2.0+ / Java 21 |
| **SDK Compatibility** | Min SDK 24 (Android 7.0) / Target SDK 37 |
| **Dependency Injection** | Hilt (Dagger) + KSP |
| **UI Framework** | Material 3, XML ViewBinding, ConstraintLayout, Lottie, Facebook Shimmer |
| **Network & Serialization** | Retrofit 2, OkHttp 4, Kotlinx Serialization |
| **Local Storage** | Room 2.6 + SQLCipher, Jetpack DataStore Preferences, KeyStore |
| **Async & Concurrency** | Kotlin Coroutines, StateFlow, SharedFlow, WorkManager |
| **Code Quality & Gates** | Detekt, KtLint, Kover, Baseline Profiles |

---

## 📦 Consuming `:core` via JitPack

The `:core` module contains all reusable architectural foundation code. You can include it in any external Android app without copying code:

### 1. Add JitPack Repository
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

### 2. Add Library Dependency
In your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.ThanhNg224:AndroidXmlBase:v2.0.0")
}
```

Check available tags and builds on [JitPack: ThanhNg224/AndroidXmlBase](https://jitpack.io/#ThanhNg224/AndroidXmlBase).

### 3. Configure Your Own Module

`:core` is Hilt-based and XML/ViewBinding-based, so a consuming module needs the same plumbing:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    defaultConfig { minSdk = 24 }                 // :core's minSdk
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { viewBinding = true }          // needed for BaseActivity/BaseFragment
}

dependencies {
    implementation("com.github.ThanhNg224:AndroidXmlBase:v2.0.0")
    ksp("com.google.dagger:hilt-compiler:<version>")

    testImplementation(testFixtures("com.github.ThanhNg224:AndroidXmlBase:v2.0.0"))
}
```

Your `Application` needs `@HiltAndroidApp`; Activities and Fragments need `@AndroidEntryPoint`.
Retrofit, OkHttp, coroutines, AppCompat, Fragment, lifecycle-viewmodel, Material and Timber arrive
transitively as `api` dependencies — you do not need to redeclare them to use `:core`'s API.

---

## ⚙️ Wiring `:core` Into Your App

### Required: supply an `ApiConfig`

`:core` deliberately ships **no** base URL. Injecting `Retrofit` or `OkHttpClient` without a binding
throws with a message showing exactly this module:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppNetworkModule {
    @Provides
    @Singleton
    fun provideApiConfig() =
        ApiConfig(
            baseUrl = BuildConfig.API_BASE_URL,
            enableLogging = BuildConfig.DEBUG,
            readTimeoutSeconds = 20,          // per-timeout overrides are optional
        )
}
```

### Optional: enable real token refresh

`TokenAuthenticator` retries a 401 once with a fresh token, but only if you bind an
`AuthTokenRefresher` — refreshing needs an API contract `:core` can't know. Concurrent 401s share a
single refresh, and the result is persisted through `AuthSession`.

```kotlin
class MyTokenRefresher @Inject constructor(
    private val api: AuthApi,                 // built on a plain client, NOT the :core one,
) : AuthTokenRefresher {                      // or you recurse back into this same auth flow
    override suspend fun refresh(refreshToken: String?): String? =
        refreshToken?.let { runCatching { api.refresh(it).accessToken }.getOrNull() }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    abstract fun bindRefresher(impl: MyTokenRefresher): AuthTokenRefresher
}
```

Read and write the tokens themselves through the injectable `AuthSession`
(`getAccessToken()`, `setTokens(...)`, `clear()` on logout).

### Optional: add languages

`AppLanguage` is a data class, not a closed enum, so you can ship locales `:core` has no strings for.
Declare them in your own `@xml/locales_config` and bind the list:

```kotlin
@Provides
@Singleton
fun provideSupportedLanguages() =
    SupportedLanguages(
        AppLanguage.BUILT_IN + AppLanguage("ja", R.string.language_japanese),
    )
```

### Screens

```kotlin
@AndroidEntryPoint
class ProfileActivity : BaseActivity<ActivityProfileBinding>() {
    private val viewModel: ProfileViewModel by viewModels()
    private val userId: String by intentExtra(EXTRA_USER_ID)   // type-safe extras

    override fun inflateBinding(inflater: LayoutInflater) = ActivityProfileBinding.inflate(inflater)

    override fun onBindingReady(savedInstanceState: Bundle?) {
        viewModel.state.collectOnStarted(::render)             // lifecycle-aware, STARTED
        viewModel.effect.collectOnStarted(::handleEffect)      // one-shot effects
    }
}
```

ViewModels extend `StateViewModel<S, E, F>` and get `state: StateFlow<S>`, one-shot
`effect: Flow<F>`, `setState { }` and `sendEffect(...)`. See
[docs/FEATURE_TEMPLATE.md](docs/FEATURE_TEMPLATE.md) for a full vertical slice, and
[docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) for the theme, `core_`-prefixed resources and
components (`FrameButton`, `ShadowLayout`, `ThemedSwitch`, `StyledSnackbar`, `PromptDialogFragment`,
`FullScreenLoaderView`).

> **Note on resources:** every `:core` layout, anim, drawable, raw asset and styleable is
> `core_`-prefixed so your own same-named resources can't silently override them. Styles keep
> `TextAppearance.AndroidXmlBase.*` / `Theme.AndroidXmlBase.*` naming.

> **Note on WorkManager:** `:core` leaves WorkManager's default initializer in place. If your app
> supplies its own `Configuration.Provider`, remove the default initializer in *your* manifest.

---

## 📂 Project Architecture

```
AndroidXmlBase/
├── core/                                               # Reusable Library Module (Published to JitPack)
│   └── src/main/java/com/thanhng224/androidxmlbase/core/
│       ├── architecture/                               # Base ResultState, DomainResult & StateViewModel
│       ├── di/                                         # Hilt DI module bindings
│       ├── localization/                               # Multi-language LocaleManager
│       ├── logging/                                    # Production Timber tree setup
│       ├── navigation/                                 # Activity & Fragment transition navigators
│       ├── network/                                    # ApiClient, Auth Interceptors & File Transfer Clients
│       ├── startup/                                    # App Startup Initializers (Timber, Theme, DB Warmup)
│       ├── storage/                                    # Room + SQLCipher, EncryptedSecureStore, DataStore
│       ├── time/                                       # Monotonic clocks
│       ├── ui/                                         # Base Activity/Fragment, Custom Components, Delegates
│       └── work/                                       # Background WorkManager workers
│
└── app/                                                # Application Shell & Sample Showcase
    └── src/main/java/com/example/androidxmlbase/
        ├── AndroidXmlBaseApplication.kt                # Application entry point
        ├── MainActivity.kt                             # Shell container & bottom navigation
        ├── appshell/                                   # App shell destinations (Home)
        ├── feature/                                    # Concrete feature modules (Settings)
        └── sample/                                     # Reference implementations & UI design system
```

---

## 💻 Building & Verification Commands

All quality gates and build tasks can be executed via Gradle wrapper:

```bash
# Build APKs
./gradlew assembleDebug           # Build Debug APK
./gradlew assembleRelease         # Build Release APK

# Testing & Quality Gates
./gradlew test                    # Run JVM unit tests across all modules
./gradlew check                   # Execute complete quality gate (Tests, KtLint, Detekt, Kover)

# Formatting
./gradlew ktlintFormat            # Auto-format Kotlin source code according to project standards
./gradlew detekt                  # Run static code analysis
```

---

## 🏷️ Cutting a Release

`:core`'s version comes from `VERSION` (set by JitPack from the tag) falling back to
`VERSION_NAME` in `core/gradle.properties`. To release:

```bash
# 1. update CHANGELOG.md and bump VERSION_NAME in core/gradle.properties to match
# 2. verify the gate and that the published artifact assembles
./gradlew check :core:assembleRelease
# 3. tag with the same value and push
git tag v2.0.0 && git push origin v2.0.0
```

JitPack builds the tag using `jitpack.yml` (pinned to JDK 21). Semantic versioning applies to
`:core`'s **public** API only — `internal` declarations are not part of the contract.

Test a candidate against a real consumer before tagging:

```bash
./gradlew :core:publishToMavenLocal      # then add mavenLocal() in the consumer project
```

---

## 📄 Engineering Documentation

For detailed guidelines and architectural specifications, refer to the `docs/` folder:

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) – Layering, state management, and dependency rules.
- [CORE_MODULES.md](docs/CORE_MODULES.md) – Structure and encapsulation rules for `:core`.
- [FEATURE_TEMPLATE.md](docs/FEATURE_TEMPLATE.md) – Worked example of a full feature slice.
- [DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) – Theme, tokens, and component catalogue.
- [STANDARD.md](docs/STANDARD.md) – Coding conventions, naming, and formatting rules.
- [GIT_FLOW.md](docs/GIT_FLOW.md) – Branching strategy, commit conventions, and PR workflow.
- [MODERNIZATION.md](docs/MODERNIZATION.md) – Rolling plan for hardening `:core` as a library, with what is already correct and why.
- [CHANGELOG.md](CHANGELOG.md) – Released versions and breaking changes.

Licensed under the [MIT License](LICENSE).
