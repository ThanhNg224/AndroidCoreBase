# Migrating from v1 to v2

`AndroidCoreBase` v2 is a breaking release: `:core` and the new `:core:ui-compose` module apply no
Hilt/KSP plugin and ship no DI annotations, Settings moved into the app's single-Activity navigation
graph, and several v1 marker types were removed outright rather than deprecated. There is no
compatibility shim — a v1 consumer must update call sites at the points below. See
`docs/CORE_V2_DESIGN.md` for the full architecture decision record this migration follows.

## Every removed API and its exact replacement

| Removed (v1) | Replaced by (v2) |
|---|---|
| `StateViewModel<S, E, F>` / `UiEffect` (`Channel`-backed one-shot effects) | Plain `androidx.lifecycle.ViewModel` + `MutableStateFlow<S>`/`StateFlow<S>`. A transient one-shot request (snackbar, navigation) is a `pendingMessages: List<PendingMessage>` field on `S` itself, acknowledged via `onMessageHandled(id)` once shown. |
| `core.architecture.UiState`/`UiEvent`/`UiEffect` (empty marker interfaces) | Nothing to implement — a screen's state/event classes are plain data/sealed types with no shared supertype. |
| `core.architecture.UseCase<in P, R>` | A plain class with `operator fun invoke(...)`, in whatever shape (sync, suspend, `Flow`-returning) the use case actually needs. No shared interface. |
| `DomainResult<out T>` / `AppError` (generic domain result/error) | Feature-owned result/error types (e.g. `sample/demo`'s `WeatherResult`/`WeatherError`), and `ApiResult`/`ApiFailure` in the data layer (`core/network`, renamed from the old `ApiResult`'s `HttpError`/`NetworkError`/`ParseError` cases to `ApiFailure.Http`/`Network`/`Serialization`). |
| `ResultState<T>` / `ResultRenderState` / `renderResultState` / `bindResultState` | An explicit sealed state per screen (e.g. `DemoWeatherState`, `DesignSystemDemoState`), rendered with an exhaustive `when`. `:core` publishes no generic result-rendering mechanism. |
| `FullScreenLoaderView` / `PromptDialogFragment` | Removed outright (zero real consumers once the generic result renderer was removed). Build your own loading/prompt UI, or keep using `BaseDialogFragment` directly if you need a themed dialog host. |
| `TransitionActivity` / `TransitionAction` / action multibinding | Settings (and any screen that used to launch a transition) is a `Fragment` destination in the single-Activity `NavController` graph. A locale change persists through the repository first, then applies via `LocaleManager` directly — no activity-recreating transition host. |
| Core Hilt modules (`core/di/*`, `core/ui/theme/ThemeModule`) | App-owned Hilt modules calling `:core`'s public factories/constructors (`AppDispatchers.default()`, `SettingsStoreFactory.create(...)`, `SecureStoreFactory.encrypted(...)`, `ThemeManager.create(...)`, `NetworkClientFactory.create*(...)`, `AuthSession(...)`, `DbPassphraseProvider(...)`, `LocaleManager(...)`). See `README.md`'s "Wiring `:core` into your app". |
| `EncryptedSecureStore` | `EncryptedFileSecureStore` (internal; obtain via `SecureStoreFactory.encrypted(context, dispatchers)`). Same Keystore-backed AES-256-GCM envelope, now via `AtomicFile` instead of the old file-write path. |
| `core/network/connectivity` (`ConnectivityChecker`, `ConnectivityInterceptor`, `NoConnectivityException`) | Removed outright — unused in every real consumer. Add your own `ConnectivityManager`-backed check if you need one. |
| `core/time/ElapsedRealtimeClock` | Removed outright — unused. Call `android.os.SystemClock.elapsedRealtime()` directly if you need it. |
| `core/ui/text/StringProvider` / `AndroidStringProvider` | Removed outright — unused (`UiText.resolve(context)` already covers resolving a string resource from presentation code). |
| `androidx.startup` initializers (`TimberInitializer`, `ThemeApplyInitializer`, `LocaleContextInitializer`, `AppStartupEntryPoint`) | Explicit orchestration in your own `Application.onCreate()` — see `app/src/main/java/com/example/androidcorebase/startup/AppStartupCoordinator.kt` and `AndroidCoreBaseApplication.kt` for the reference shape (bounded-timeout theme apply, Timber tree planted based on the app's own `ApplicationInfo.FLAG_DEBUGGABLE`). |
| `core/logging/ReleaseTree` | Moved to the app (`app/src/main/java/com/example/androidcorebase/logging/AppReleaseTree.kt`) — a published library should not own your app's log filtering policy. |
| `core/work/HeartbeatWorker` | Moved to the app (`app/src/main/java/com/example/androidcorebase/work/HeartbeatWorker.kt`) as a reference `CoroutineWorker` shape to copy, not a class `:core` ships. |
| `core/navigation/ActivityNavigator` / `ActivityDestination` / `NavigationOptions` / `TransitionType` | Removed outright — their only consumer was the old `SettingsActivity`, which the Fragment/`NavController` redesign replaced. Use `NavController.navigate(...)` for in-app navigation. |
| Main-artifact Compose helpers (`AndroidCoreBaseTheme`, `ComposeView.setThemedContent`, `BaseComposeActivity`) | Moved to the new, optional `AndroidCoreBase-ui-compose` published module/coordinate — add it only if you write Compose screens. |
| Passive-manifest violations: `ACCESS_NETWORK_STATE`, `INTERNET`, `androidx.startup` provider entries, `TransitionActivity`'s manifest entry | `:core` and `:core:ui-compose` publish a manifest with no permissions, components, providers, or services at all. Declare `INTERNET` (and AppCompat's locale auto-storage metadata, if you want it) in your own app manifest. |

## Dependency coordinates

```kotlin
dependencies {
    implementation("com.github.ThanhNg224.AndroidCoreBase:AndroidCoreBase:v2.0.1")

    // New, optional: only if you write Compose screens.
    implementation("com.github.ThanhNg224.AndroidCoreBase:AndroidCoreBase-ui-compose:v2.0.1")
}
```

## Required app-side changes

1. **Add your own Hilt module** wiring `:core`'s public factories (see `README.md`'s "Wiring `:core`
   into your app" table and worked example). There is no core-provided Hilt module to remove a
   dependency on — v1's `core/di/*` classes are gone, not deprecated.
2. **Move your app's own DI-framework plugins/dependencies unchanged** — v2 doesn't touch what DI
   framework `:app` uses, only removes `:core`'s own.
3. **Replace any `StateViewModel`/`UiEffect` subclass** with a plain `ViewModel` + `StateFlow` +
   (if you had one-shot effects) a `pendingMessages`-style queue on your state class.
4. **Replace any `DomainResult`/`AppError` usage** with your own feature-owned result/error type.
5. **Remove any dependency on `TransitionActivity`** and move the screen it hosted into your app's
   own `NavController` graph as a `Fragment` destination.
6. **Declare `INTERNET`** (and AppCompat's locale auto-storage `<service>` metadata, if you rely on
   it) in your own app's `AndroidManifest.xml` — `:core`'s manifest no longer declares them.
7. **Add Compose separately** if you use `AndroidCoreBaseTheme`/`setThemedContent`/`BaseComposeActivity`:
   depend on `AndroidCoreBase-ui-compose` and apply the Compose compiler plugin in every module that
   declares or calls `@Composable` code.

## What did *not* change

- `SettingsStore`/`SettingsKey`, `SecureStore`/`SecureStoreKey`, `ApiClient`/`ApiConfig`,
  `FileTransferClient`, `AuthSession`/`AuthTokenProvider`/`AuthTokenRefresher`, `LocaleManager`/
  `AppLanguage`, `ThemeManager`/`AppTheme`, `UiText`, the `Base*` UI hosts, `intentExtra`/`fragmentArg`
  delegates, and the `ui/components`/`ui/drawable`/`ui/window` helpers — same contracts, same
  behavior, just constructed via a public constructor/factory instead of Hilt injection.
- `:core`'s `resourcePrefix = "core_"` and every existing public `core_`-prefixed resource name.
- `:core`'s minSdk (24) and package name (`com.thanhng224.androidcorebase.core`).
