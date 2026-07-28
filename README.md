# AndroidXmlBase

A production-ready, state-of-the-art Android template project utilizing **XML layouts**, **ViewBinding**, **MVVM**, and **Clean Architecture**. This project is optimized for speed, maintainability, and enterprise-grade scalability.

---

## 🚀 Key Features

* **Clean Architecture**: Strictly separated layers (Presentation, Domain, Data) with clean package structures.
* **Hilt Dependency Injection**: Automated DI module bindings set up at package levels for clean encapsulation.
* **Encrypted Room Database**: Secured local database powered by Room and SQLCipher, utilizing runtime Keystore-stored passphrases.
* **Secure Key-Value Store**: Built-in Android KeyStore-backed `EncryptedSecureStore` implementing safe cryptographic operations.
* **Per-App Locale & Language**: Built-in compatibility with Android 13's native per-app language settings, utilizing Google Jetpack compatibility.
* **Unified Theme System**: Automated Dark/Light/System theme toggles integrating Jetpack DataStore, featuring zero-flash startup loading.
* **OkHttp Token Authenticator**: Self-healing token refresh middleware to intercept expired sessions (401) and retry API calls seamlessly.
* **Type-Safe UI Delegates**: Non-deprecated, runtime-safe property delegates (`intentExtra`, `fragmentArg`) to load bundle extras warning-free.
* **Material 3 Design System**: Styled M3 themes, custom components (SegmentedButton, IconButton), and Facebook Shimmer placeholder loaders.
* **Strict Quality Gates**: Integrated static code analysis (Detekt), style formatters (KtLint), and code coverage tracking (Kover).

---

## 🛠️ Gradle Commands

Build and test commands configured in the template:

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew test                 # Run JVM Unit Tests
./gradlew check                # Full quality gate (unit tests, ktlint, detekt, Kover)
./gradlew :app:ktlintFormat    # Automatically format Kotlin styles
```

---

## 📦 Using `:core` via JitPack

The `:core` module (architecture, network, storage, DI, and UI toolkit) is published to
[JitPack](https://jitpack.io) and can be added as a dependency from another Android project —
no need to clone or copy this repo.

1. Add the JitPack repository in your project's `settings.gradle.kts`:

    ```kotlin
    dependencyResolutionManagement {
        repositories {
            google()
            mavenCentral()
            maven("https://jitpack.io")
        }
    }
    ```

2. Add the dependency in your module's `build.gradle.kts`, pinned to a released tag:

    ```kotlin
    dependencies {
        implementation("com.github.ThanhNg224:AndroidXmlBase:v1.0.0")
    }
    ```

3. Check available tags/builds at https://jitpack.io/#ThanhNg224/AndroidXmlBase. The first
   request for a new tag takes a few minutes while JitPack builds it; a short commit hash also
   works for an unreleased snapshot.

---

## 📂 Architecture Layout

The project is split across two Gradle modules: `:app` (the application, product/sample code)
and `:core` (the reusable foundation, published to JitPack — see above).

```
core/src/main/java/com/thanhng224/androidxmlbase/core/
├── architecture/                # Base result states & ViewModels
├── di/                          # Dependency injection module bindings
├── localization/                # Multi-language locale manager
├── logging/                     # Timber tree setup
├── navigation/                  # Activity and fragment transition navigators
├── network/                     # Api clients, file transfers, and Token Authenticators
├── startup/                     # App Startup initializers (Timber, theme, locale, DB warmup)
├── storage/                     # Database (SQLCipher), secure store, and Datastore preferences
├── time/                        # Monotonic clocks
├── ui/                          # Base classes, custom components, and type-safe delegates
└── work/                        # Background WorkManager jobs

app/src/main/java/com/example/androidxmlbase/
├── AndroidXmlBaseApplication.kt
├── MainActivity.kt              # App shell and top-level navigation
├── appshell/                    # Shell-owned destinations such as Home
│   └── home/
├── feature/                     # Product capability scopes
│   └── settings/                # Canonical product feature
└── sample/                      # Reference implementations, not product features
    ├── demo/                    # Clean Architecture data/state sample
    └── designsystem/            # Reusable UI showcase
```
