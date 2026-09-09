# FEATURE_TEMPLATE.md

A step-by-step guide to adding a new product feature to this base project. A feature is a vertical slice for one capability; it can own one or more screens. Product capabilities live in `feature/<name>/`. `feature/settings` is the canonical single-screen product feature; `sample/demo` exercises the data/network path; `sample/designsystem` is a developer-facing reference showcase with no `data/` or domain use cases beyond a synchronous plain `ViewModel`.

Read `docs/CORE_MODULES.md` alongside this doc for the API surface of everything a feature is built on, and `docs/DESIGN_SYSTEM.md` for the UI tokens/components a screen's layout should use.

## 1. Folder layout

A feature lives under `app/src/main/java/com/example/androidcorebase/feature/<name>/` with up to three top-level packages. Not every feature needs all three — see section 3 for when `data/` earns its place. Do not create a root-level `screens/` package: a screen belongs beside the domain and data code of the feature that owns it.

```
sample/demo/
  domain/
    repository/
      DemoRepository.kt              # interface: fun observeCount(): Flow<Int>, suspend fun saveCount(count: Int), suspend fun fetchWeather(): WeatherResult
    usecase/
      IncrementCounterUseCase.kt      # plain class, sync business rule, no I/O
      ObserveDemoCountUseCase.kt      # plain class, Flow-returning
      SaveDemoCountUseCase.kt         # plain class, operator fun invoke(count: Int)
      FetchDemoWeatherUseCase.kt      # plain class, operator fun invoke(): WeatherResult
    model/
      WeatherResult.kt                 # feature-owned sealed Success/Failure + WeatherError, not a core type
  data/
    repository/
      DemoRepositoryImpl.kt           # implements DemoRepository, owns a feature-private SettingsKey
    datasource/
      DemoApiService.kt               # Retrofit interface
      DemoRemoteDataSource.kt         # interface + DemoRemoteDataSourceImpl, wraps ApiClient.execute
    dto/
      DemoWeatherResponseDto.kt        # @Serializable wire model
    mapper/
      DemoWeatherMapper.kt             # DemoWeatherResponseDto -> DemoWeather (domain model)
  presentation/
    state/
      DemoUiState.kt, DemoUiEvent.kt, PendingDemoMessage.kt, DemoWeatherState.kt
    viewmodel/
      DemoViewModel.kt
    ui/
      DemoFragment.kt
  di/
    DemoModule.kt                    # Hilt bindings and feature-local Retrofit service provider
```

Notes on what's *not* here, on purpose:
- No `domain/entity` package beyond what a feature genuinely needs. `DemoRepository`'s methods return primitives or feature-owned model types (`WeatherResult`), never a core marker type — add a richer domain model only when it's genuinely richer than what the DTO/primitive already expresses.
- `sample/designsystem` has only `presentation/` — no `domain/`, no `data/`. It is a pure UI-state showcase (see `DesignSystemViewModel`): its `onEvent` sets state synchronously with no repository or use case involved at all. That's a valid shape for a product feature that never reads or writes real data — don't force empty `domain/`/`data/` packages onto it just to match the folder template.

### Canonical product feature: `feature/settings`

`feature/settings` is the reference for a real product capability with one screen. It has a feature-level `SettingsRepository` interface and `SettingsRepositoryImpl`, plus focused use cases for observing/persisting theme and reading/applying the current language. Its presentation package therefore stays flat:

```
feature/settings/
  presentation/
    state/          # SettingsUiState (+ PendingSettingsMessage), SettingsUiEvent
    viewmodel/      # SettingsViewModel
    ui/             # SettingsFragment
  domain/
  data/
  di/
```

`SettingsFragment` is a `NavController` destination in the app's single-Activity nav graph (`main_navigation.xml`), reached from the shared top app bar's overflow menu — there is no separate Settings Activity or transition host. It renders a grouped settings list; theme and language are values on that screen, so they open single-choice dialogs instead of artificial child screens. The repository adapts reusable app-wide services (`ThemeManager` and `LocaleManager`) rather than duplicating their persistence/platform logic. Selecting a language calls `SettingsRepository.setLanguage`, which persists through `SettingsStore` first and only then applies the locale via `LocaleManager` — the ViewModel updates its committed `language` state only after that call succeeds, and queues a `PendingSettingsMessage` on failure. See "Settings and Locale Mutation" in `docs/CORE_V2_DESIGN.md`.

### When a feature has more than one screen

`sample/demo` and `sample/designsystem` each currently have one screen, so their flat `presentation/ui`, `presentation/viewmodel`, and `presentation/state` packages are intentional. Adding a `presentation/demo/` or `presentation/designsystem/` level now would add naming noise without creating a boundary.

Before adding a second screen to a feature, move the first screen's UI host, ViewModel, and `UiState`/`UiEvent` together into a named presentation package, then add the second screen beside it:

```
feature/auth/
  presentation/
    login/
      LoginFragment.kt
      LoginViewModel.kt
      LoginUiState.kt
      LoginUiEvent.kt
    otp/
      OtpFragment.kt
      OtpViewModel.kt
      OtpUiState.kt
      OtpUiEvent.kt
    components/                 # only if shared by two or more auth screens
  domain/
  data/
  di/
```

Keep feature-level use cases, repositories, data sources, and DI bindings outside the screen packages when they serve more than one screen. Keep screen-only mappers or UI models with that screen. If the new screen has no meaningful domain/data ownership in common with the existing feature, create a separate feature rather than forcing both under the same navigation flow.

Migration is mechanical and should be performed only when the second screen is introduced: move the existing screen's presentation files as one change, update package/import references, run the affected unit tests, and avoid changing runtime behavior in the structural commit.

## 2. Wiring a screen end to end

The real call chain in `sample/demo`, read bottom-to-top from where a tap originates to where data comes back:

```
DemoFragment (@AndroidEntryPoint, extends BaseFragment<FragmentDemoBinding>)
  -> DemoViewModel (@HiltViewModel, plain ViewModel with MutableStateFlow<DemoUiState>)
    -> IncrementCounterUseCase          (plain sync class — no repository)
    -> SaveDemoCountUseCase             (plain class, one suspend operation)
    -> FetchDemoWeatherUseCase          (plain class, returns WeatherResult)
    -> ObserveDemoCountUseCase          (plain class, returns Flow<Int>)
      -> DemoRepository (domain interface) / DemoRepositoryImpl (data)
        -> SettingsStore                (persistence, via core/storage — constructed with SettingsStoreFactory in app/di)
        -> DemoRemoteDataSource / DemoApiService / ApiClient (network, via core/network)
```

Concretely, in `DemoFragment.onBindingReady`:
1. `binding.btnIncrement.setOnDebouncedClickListener { viewModel.onEvent(DemoUiEvent.IncrementClicked) }` — a `FrameButton` (see `docs/DESIGN_SYSTEM.md`), debounced via `core.ui.base.setOnDebouncedClickListener` since it's the control most likely to be rapid-tapped.
2. `viewModel.state.collectOnStarted { ... }` renders `count`, the typed live-weather state into `tvCount`/`tvWeather`, and the head of `state.pendingMessages` (if any) as a Snackbar.

In `DemoViewModel`:
- `init` launches the persisted-count collector and a current-weather refresh. The weather path calls `fetchDemoWeather()`, maps the returned `WeatherResult` to `DemoWeatherState`, and can be triggered again from the Refresh weather control. The Open-Meteo request is a real, keyless API call for Ho Chi Minh City; its DTO stays in `data/` and only `DemoWeather` crosses into domain/presentation.
- `onIncrementClicked()` guards on `isInitialCountLoaded` (see the comment in the real file — this exists specifically to avoid computing the next count from the constructor-default `0` while the real DataStore read is still in flight, which would clobber the persisted value with a stale increment), calls `incrementCounter(state.value.count)`, updates state, fires `saveDemoCount` async, and enqueues a `PendingDemoMessage` (with a `DemoMessageAction.ResetCounter` action) if the result is capped.
- A transient message is a value in `state.pendingMessages: List<PendingDemoMessage>`, not a separate `Channel`-backed effect. The Fragment shows only the **head** of that list and acknowledges it (`onMessageHandled(id)`, or `onMessageAction(id)` if the Snackbar's action button was tapped) once shown — the queue survives a configuration change because it lives in the same `StateFlow` as the rest of the screen's state, unlike a `Channel` which can silently drop or duplicate an emission across recreation.

Hilt is `:app`'s composition root; `:core` has none. Core-provided contracts (`SettingsStore`, `ApiClient`, `ThemeManager`, …) are wired once in `app/src/main/java/com/example/androidcorebase/di/` (`AppCoreModule.kt`, `AppNetworkModule.kt`) by calling `:core`'s public factories; product feature bindings and feature-specific Retrofit services live beside the feature (`feature/<name>/di`). `sample/demo/di/DemoModule` demonstrates the same rule for reference code. Use constructor injection for repositories, data sources, use cases, and ViewModels. Add a feature Hilt module only when Hilt needs an interface binding (`@Binds`) or framework construction (`@Provides`). Do not provide a feature API service from `app/di`.

`DemoFragment` obtains its `ViewModel` via:
```kotlin
@AndroidEntryPoint
class DemoFragment : BaseFragment<FragmentDemoBinding>() {
    private val viewModel: DemoViewModel by viewModels()
}
```

## 3. When to add a `data/` layer

**Rule: don't add `data/` (or a repository) until the feature actually reads or writes real data.**

This is not a stylistic preference — it's this codebase's own precedent. `sample/demo` originally shipped with *no* `data/` package and no repository at all, before that layer was added the moment the counter needed to survive process death and the weather demo needed a real Open-Meteo request. If your new feature's screen is purely local UI state with no persistence and no API call — like `sample/designsystem` — it should contain only `presentation/`, with no `domain/` or `data/`.

## 4. Modeling a UseCase

There is no shared `UseCase<in P, R>` marker interface in `:core` — a use case is just a plain class with `operator fun invoke(...)`, whatever suspend/sync/`Flow` shape actually fits:

- `SaveDemoCountUseCase` and `FetchDemoWeatherUseCase` each do exactly one `suspend` operation and return exactly one result — a plain class with a `suspend operator fun invoke(...)` is enough; there is no interface to implement.
- `ObserveDemoCountUseCase` returns a `Flow<Int>` (an ongoing stream, not a single suspend result); it's a plain class with `operator fun invoke(): Flow<Int> = repository.observeCount()`.
- `IncrementCounterUseCase` is a pure synchronous business rule with no I/O at all (`operator fun invoke(currentCount: Int): IncrementResult`).

Don't invent a shared use-case interface just for consistency — pick the shape (sync/suspend/`Flow`) that matches what the use case actually does, and let a plain class express it directly.

## 5. Checklist for a new feature

- Does this screen need a ViewModel, or is it static? If static, you may not need `presentation/viewmodel` at all — but every screen in this codebase so far has used a `StateFlow`-backed `ViewModel`, even `sample/designsystem`'s purely-synchronous one; only skip it if there's truly no state or events.
- Does it read or write data? If not, skip `data/` (and `domain/repository`) entirely — see section 3.
- Does it need a new setting? Define a feature-private `SettingsKey` inside the feature's own repository implementation (see `DemoRepositoryImpl`'s private counter key), not in `core.storage.settings.AppSettingsKeys` — that object is reserved for genuinely app-wide keys (language, theme, first-open timestamp, debug logging). See section 6, anti-pattern 2.
- Does a use case return a `Flow` or do pure sync work with no I/O? Keep it a plain callable class with `operator fun invoke`. Does it do exactly one suspend operation with one result? Same: a plain class, `suspend operator fun invoke`. See section 4.
- Is the screen an equally important top-level area? Add it as a Fragment destination in the app shell only when it belongs in the 3-5 item bottom-navigation taxonomy. A feature name and a bottom-navigation item are separate decisions. A screen reached from elsewhere (like Settings, from the top app bar) is still a plain Fragment destination in `main_navigation.xml` — not a separate Activity.
- New screen? Extend `core.ui.base.BaseFragment<VB>` (or `BaseBindingActivity<VB>` only for a genuinely new top-level Activity — most screens are Fragments in the existing single-Activity shell), implement `inflateBinding(inflater)`, and do all view/ViewModel wiring in `onBindingReady(...)`. Do not hand-wire ViewModel factories; use Hilt + `by viewModels()`.
- New Dialog? Prefer `BaseDialogFragment<VB>` or `BaseBottomSheetDialogFragment<VB>` and annotate concrete classes with `@AndroidEntryPoint` when they inject dependencies.
- Need a secret/token? Use `SecureStore`/`SecureStoreKeys`, not `SettingsStore`.
- Need upload/download/streaming? Inject a `FileTransferClient` (constructed via `NetworkClientFactory.createFileTransferClient` in `app/di`); do not hand-roll OkHttp calls in a feature.
- Buttons: use `core.ui.components.FrameButton` with design tokens from `docs/DESIGN_SYSTEM.md`, not a plain `<Button>` or hardcoded colors. Debounce the control most likely to be rapid-tapped with `View.setOnDebouncedClickListener` (not necessarily every control — one real usage per screen has been this codebase's bar so far).
- Layout dimensions: use the fixed tokens in `core/src/main/res/values/dimens.xml` (`@dimen/core_space_<n>`, `core_radius_<n>`, `core_size_<n>`, `core_stroke_width`, `core_text_size_<n>`), never a literal `16dp`/`14sp` (see `docs/DESIGN_SYSTEM.md` for the convention and its rationale).
- Loading/success/error UI: model an explicit sealed state for the screen (like `DemoWeatherState` or `DesignSystemDemoState`) and render it with an exhaustive `when`. `:core` publishes no generic result-rendering mechanism — a persistent error renders inline in the layout the screen already owns; a transient message goes through the `pendingMessages` queue from section 2, never a full-screen overlay dialog `:core` shows for you.
- All user-facing strings go through `strings.xml` (and get a `values-vi/strings.xml` translation — see `sample/demo`'s `demo_title`/`increment`).

## 6. Anti-patterns (do not do these)

These are drawn from real decisions made across this project's phases — not generic advice.

1. **Adding a repository/data source before there's real data to fetch or persist.** If you're writing a `FooRepository` interface whose only implementation returns hardcoded/in-memory values with no real backing store, you're speculatively building an abstraction with nothing to abstract yet — wait until the feature actually reads or writes real data (section 3).

2. **Putting a feature-specific `SettingsKey` into `AppSettingsKeys`.** `core.storage.settings.AppSettingsKeys` holds only app-wide keys (theme, language, first-open timestamp, debug logging) — feature-specific keys belong to the feature that owns them. `DemoRepositoryImpl`'s counter key is a private constant inside the repository implementation itself, not in the shared keys object. Follow that pattern for any new per-feature persisted value.

3. **Inventing a shared `UseCase<P, R>` interface "for consistency".** A use case is a plain class with `operator fun invoke`, in whatever shape (sync, suspend, `Flow`) actually matches what it does — see section 4. Don't wrap a `Flow` in a suspend function, or force a synchronous rule through a suspend signature, just to satisfy a shared interface that doesn't exist in `:core` v2 on purpose.

4. **Launching a transient message through anything other than the screen's own `pendingMessages` state.** A `Channel`-backed effect can silently drop an emission the collector wasn't attached to yet, or re-fire one across a configuration change. Model it as acknowledged state instead (section 2), the same way `DemoViewModel`/`SettingsViewModel` do.

5. **Recreating manual ViewModel factories now that Hilt exists.** Use constructor injection, `@HiltViewModel`, `@AndroidEntryPoint`, and feature-local Hilt modules for bindings. A custom `ViewModelProvider.Factory` should be rare and justified.

6. **Adding a new custom button variant (`CardButton`, `LinearButton`, `ConstraintButton`, etc.) before a real screen needs that specific shape.** This codebase is scoped to `ButtonStyleDelegate` + one concrete variant (`FrameButton`). Add a new variant only when a real screen needs that specific shape — not speculatively.
