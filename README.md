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
* **Self-Healing Network Layer**: **Retrofit + OkHttp** client with built-in token refresh interceptor/authenticator, file upload/download progress tracking, and offline status interceptors.
* **Per-App Locale & Dynamic Theme System**: Native per-app language selection (Android 13+ / Jetpack Compat) and zero-flash Light/Dark/System theme management backed by **Jetpack DataStore**.
* **Encapsulated UI Toolkit**: Type-safe property delegates (`intentExtra`, `fragmentArg`), result state overlay renderers, custom Material 3 components, and smooth Lottie/Shimmer loading states.
* **Strict Engineering & Quality Gates**: Integrated **Detekt** (static analysis), **KtLint** (style formatting), and per-module **Kover** code coverage verification rules (80%+ threshold).

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
    implementation("com.github.ThanhNg224:AndroidXmlBase:v1.0.0")
}
```

Check available tags and builds on [JitPack: ThanhNg224/AndroidXmlBase](https://jitpack.io/#ThanhNg224/AndroidXmlBase).

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

## 📄 Engineering Documentation

For detailed guidelines and architectural specifications, refer to the `docs/` folder:

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) – Layering, state management, and dependency rules.
- [CORE_MODULES.md](docs/CORE_MODULES.md) – Structure and encapsulation rules for `:core`.
- [STANDARD.md](docs/STANDARD.md) – Coding conventions, naming, and formatting rules.
- [GIT_FLOW.md](docs/GIT_FLOW.md) – Branching strategy, commit conventions, and PR workflow.
