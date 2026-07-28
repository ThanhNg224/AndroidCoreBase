# FEATURE_TEMPLATE.md

A step-by-step guide to adding a new product feature to this base project. A feature is a vertical slice for one capability; it can own one or more screens. Product capabilities live in `feature/<name>/`. `feature/settings` is the canonical single-screen product feature; `sample/demo` exercises the data/network path; `sample/designsystem` is a developer-facing reference showcase with no `data/` or domain use cases beyond a synchronous `StateViewModel`.

Read `docs/CORE_MODULES.md` alongside this doc for the API surface of everything a feature is built on, and `docs/DESIGN_SYSTEM.md` for the UI tokens/components a screen's layout should use.

## 1. Folder layout

A feature lives under `app/src/main/java/com/example/androidxmlbase/feature/<name>/` with up to three top-level packages. Not every feature needs all three — see section 3 for when `data/` earns its place. Do not create a root-level `screens/` package: a screen belongs beside the domain and data code of the feature that owns it.

```
sample/demo/
    domain/
    repository/
      DemoRepository.kt              # interface: fun observeCount(): Flow<Int>, suspend fun saveCount(count: Int), suspend fun fetchWeather(): DomainResult<DemoWeather>
    usecase/
      IncrementCounterUseCase.kt      # plain class, sync business rule, no I/O
      ObserveDemoCountUseCase.kt      # plain class, Flow-returning
      SaveDemoCountUseCase.kt         # implements UseCase<Int, Unit>
      FetchDemoWeatherUseCase.kt      # implements UseCase<Unit, DomainResult<DemoWeather>>
  data/
    repository/
      DemoRepositoryImpl.kt           # implements DemoRepository, owns a feature-private SettingsKey
    datasource/
      DemoApiService.kt               # Retrofit interface
      DemoRemoteDataSource.kt         # interface + DemoRemoteDataSourceImpl, wraps ApiClient.execute
    dto/
      DemoMessageDto.kt                # @Serializable wire model
    mapper/
      DemoMessageMapper.kt             # ApiResult<DemoMessageDto>.toDomainResult(): DomainResult<String>
  presentation/
    state/
      DemoUiState.kt, DemoUiEvent.kt, DemoUiEffect.kt, DemoWeatherState.kt
    viewmodel/
      DemoViewModel.kt
    ui/
      DemoFragment.kt
  di/
    DemoModule.kt                    # Hilt bindings and feature-local Retrofit service provider
```

Notes on what's *not* here, on purpose:
- No `domain/entity` (or `domain/model`) package. `DemoRepository`'s methods return primitives (`Int`, `String` wrapped in `DomainResult`) — there is no feature-specific entity type yet that a mapper produces. Add one only when a feature's domain model is genuinely richer than what the DTO/primitive already expresses; don't create an empty passthrough entity for its own sake.
- `sample/designsystem` has only `presentation/` — no `domain/`, no `data/`. It is a pure UI-state showcase (see `DesignSystemViewModel`): its `onEvent` sets state synchronously with no repository or use case involved at all. That's a valid shape for a product feature that never reads or writes real data — don't force empty `domain/`/`data/` packages onto it just to match the folder template.

### Canonical product feature: `feature/settings`

`feature/settings` is the reference for a real product capability with one screen. It has a feature-level `SettingsRepository` interface and `SettingsRepositoryImpl`, plus focused use cases for observing/persisting theme and reading/applying the current language. Its presentation package therefore stays flat:

```
feature/settings/
  presentation/
    state/          # SettingsUiState, SettingsUiEvent, SettingsUiEffect
    viewmodel/      # SettingsViewModel
    ui/             # SettingsActivity, LanguageTransitionAction
  domain/
  data/
  di/
```

`SettingsActivity` renders a grouped settings list. Theme and language are values on that screen, so they open single-choice dialogs instead of artificial child screens. The repository adapts reusable app-wide services (`ThemeManager` and `LocaleManager`) rather than duplicating their persistence/platform logic. The feature-owned `LanguageTransitionAction` runs inside core `TransitionActivity`; the ViewModel does not apply locales directly. This keeps screen ownership and the locale-recreation safety mechanism separate.

### When a feature has more than one screen

`sample/demo` and `sample/designsystem` each currently have one screen, so their flat `presentation/ui`, `presentation/viewmodel`, and `presentation/state` packages are intentional. Adding a `presentation/demo/` or `presentation/designsystem/` level now would add naming noise without creating a boundary.

Before adding a second screen to a feature, move the first screen's UI host, ViewModel, and `UiState`/`UiEvent`/`UiEffect` together into a named presentation package, then add the second screen beside it:

```
feature/auth/
  presentation/
    login/
      LoginFragment.kt
      LoginViewModel.kt
      LoginUiState.kt
      LoginUiEvent.kt
      LoginUiEffect.kt
    otp/
      OtpFragment.kt
      OtpViewModel.kt
      OtpUiState.kt
      OtpUiEvent.kt
      OtpUiEffect.kt
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
  -> DemoViewModel (@HiltViewModel, extends StateViewModel<DemoUiState, DemoUiEvent, DemoUiEffect>)
    -> IncrementCounterUseCase          (plain sync class — no repository)
    -> SaveDemoCountUseCase             (implements UseCase<Int, Unit>)
    -> FetchDemoWeatherUseCase          (implements UseCase<Unit, DomainResult<DemoWeather>>)
    -> ObserveDemoCountUseCase          (plain class, returns Flow<Int>)
      -> DemoRepository (domain interface) / DemoRepositoryImpl (data)
        -> SettingsStore                (persistence, via core/storage)
        -> DemoRemoteDataSource / DemoApiService / ApiClient (network, via core/network)
```

Concretely, in `DemoFragment.onBindingReady`:
1. `binding.btnIncrement.setOnDebouncedClickListener { viewModel.onEvent(DemoUiEvent.IncrementClicked) }` — a `FrameButton` (see `docs/DESIGN_SYSTEM.md`), debounced via `core.ui.base.setOnDebouncedClickListener` since it's the control most likely to be rapid-tapped.
2. `viewModel.state.collectOnStarted { ... }` renders `count` and the typed live-weather state into `tvCount`/`tvWeather`.
3. `viewModel.effect.collectOnStarted { ... }` handles one-shot effects such as `DemoUiEffect.ShowMaxCountReached`.

In `DemoViewModel`:
- `init` launches the persisted-count collector and a current-weather refresh. The weather path calls `FetchDemoWeatherUseCase(Unit)`, maps the returned `DomainResult<DemoWeather>` to `DemoWeatherState`, and can be triggered again from the Refresh weather control. The Open-Meteo request is a real, keyless API call for Ho Chi Minh City; its DTO stays in `data/` and only `DemoWeather` crosses into domain/presentation.
- `onIncrementClicked()` guards on `isInitialCountLoaded` (see the comment in the real file — this exists specifically to avoid computing the next count from the constructor-default `0` while the real DataStore read is still in flight, which would clobber the persisted value with a stale increment), calls `incrementCounter(currentState.count)`, updates state, fires `saveDemoCount` async, and sends `ShowMaxCountReached` if the result is capped.

Hilt is the composition root. Core app bindings live in `core/di`; product feature bindings and feature-specific Retrofit services live beside the feature (`feature/<name>/di`). `sample/demo/di/DemoModule` demonstrates the same rule for reference code. Use constructor injection for repositories, data sources, use cases, and ViewModels. Add a feature Hilt module only when Hilt needs an interface binding (`@Binds`) or framework construction (`@Provides`). Do not provide a feature API service from `core/di`.

`DemoFragment` obtains its `ViewModel` via:
```kotlin
@AndroidEntryPoint
class DemoFragment : BaseFragment<FragmentDemoBinding>() {
    private val viewModel: DemoViewModel by viewModels()
}
```

## 3. When to add a `data/` layer

**Rule: don't add `data/` (or a repository) until the feature actually reads or writes real data.**

This is not a stylistic preference — it's this codebase's own precedent. `sample/demo` shipped in Phase 1 with *no* `data/` package and no repository at all; its `IncrementCounterUseCase` was a plain synchronous class with nothing to persist or fetch. The Phase 1 plan states the reasoning directly:

> "`sample/demo` has no `data/` package. It does not read or write any real data, so a repository or data source would be an abstraction with nothing to abstract. That layer gets added once Phase 2 (storage) or Phase 3 (network) gives the sample something to fetch or persist."

`data/repository`, `data/datasource`, `data/dto`, and `data/mapper` were added incrementally, exactly when a real need appeared: Phase 2 added `data/repository/DemoRepositoryImpl.kt` the moment the counter needed to survive process death; the weather demo adds `data/dto`, `data/datasource`, and `data/mapper` for its real Open-Meteo request. If your new feature's screen is purely local UI state with no persistence and no API call — like `sample/designsystem` — it should contain only `presentation/`, with no `domain/` or `data/`.

## 4. When a UseCase should implement `core.architecture.UseCase<in P, R>`

`UseCase<in P, R>` is a `suspend operator fun invoke(params: P): R` contract for **single-result, suspend operations**. Implement it only when that shape actually fits:

- **Implement it** — `SaveDemoCountUseCase : UseCase<Int, Unit>` and `FetchDemoWeatherUseCase : UseCase<Unit, DomainResult<DemoWeather>>` both do exactly one suspend operation and return exactly one result.
- **Don't implement it, stay a plain class** —
  - `ObserveDemoCountUseCase` returns a `Flow<Int>` (an ongoing stream, not a single suspend result); it's a plain class with `operator fun invoke(): Flow<Int> = repository.observeCount()`.
  - `IncrementCounterUseCase` is a pure synchronous business rule with no I/O at all (`operator fun invoke(currentCount: Int): IncrementResult`); forcing it into a `suspend` interface would add ceremony with zero benefit.

Don't force every use case in a feature onto the same interface just for consistency — pick the shape that matches what the use case actually does.

## 5. Checklist for a new feature

- Does this screen need a ViewModel, or is it static? If static, you may not need `presentation/viewmodel` at all — but every screen in this codebase so far has used `StateViewModel`, even `sample/designsystem`'s purely-synchronous one; only skip it if there's truly no state or events.
- Does it read or write data? If not, skip `data/` (and `domain/repository`) entirely — see section 3.
- Does it need a new setting? Define a feature-private `SettingsKey` inside the feature's own repository implementation (see `DemoRepositoryImpl`'s private `DEMO_COUNTER_COUNT` key), not in `core.storage.AppSettingsKeys` — that object is reserved for genuinely app-wide keys (language, theme, first-open timestamp, debug logging). See section 6, anti-pattern 2.
- Does a use case return a `Flow` or do pure sync work with no I/O? Keep it a plain callable class. Does it do exactly one suspend operation with one result? Implement `UseCase<in P, R>`. See section 4.
- Is the screen an equally important top-level area? Add it as a Fragment destination in the app shell only when it belongs in the 3-5 item bottom-navigation taxonomy. A feature name and a bottom-navigation item are separate decisions.
- New `Activity`? Annotate with `@AndroidEntryPoint`, extend `core.ui.base.BaseActivity<VB>`, implement `inflateBinding(inflater)`, and do all view/ViewModel wiring in `onBindingReady(savedInstanceState)`. Do not hand-wire ViewModel factories; use Hilt + `by viewModels()`.
- New Fragment/Dialog? Prefer `BaseFragment<VB>`, `BaseDialogFragment<VB>`, or `BaseBottomSheetDialogFragment<VB>` and annotate concrete classes with `@AndroidEntryPoint` when they inject dependencies.
- Need a secret/token? Use `SecureStore`/`SecureStoreKeys`, not `SettingsStore`.
- Need upload/download/streaming? Inject `FileTransferClient`; do not hand-roll OkHttp calls in a feature.
- Need Activity navigation? Inject `ActivityNavigator` and navigate with `ActivityDestination`/`NavigationOptions`.
- Buttons: use `core.ui.components.FrameButton` with design tokens from `docs/DESIGN_SYSTEM.md`, not a plain `<Button>` or hardcoded colors. Debounce the control most likely to be rapid-tapped with `View.setOnDebouncedClickListener` (not necessarily every control — one real usage per screen has been this codebase's bar so far).
- Layout dimensions: use `@dimen/_<n>sdp` / `@dimen/_<n>ssp`, never a literal `16dp`/`14sp` (see `docs/DESIGN_SYSTEM.md` for the convention and its rationale).
- Loading/success/error UI: keep domain/data failures as `DomainResult<T>` + `AppError`, then map them in presentation to a feature UI state such as `DemoWeatherState`. For pure UI demos or simple presentation-only state, `ResultState<T>` is still available; see `DesignSystemFragment.render()`, which uses `ResultState.fold(...)` for text and `.toRenderState()` for visibility.
- All user-facing strings go through `strings.xml` (and get a `values-vi/strings.xml` translation — see `sample/demo`'s `demo_title`/`increment`).

## 6. Anti-patterns (do not do these)

These are drawn from real decisions made across this project's phases — not generic advice.

1. **Adding a repository/data source before there's real data to fetch or persist.** `sample/demo` deliberately shipped with zero `data/` files in Phase 1 for exactly this reason (quoted in full in section 3). If you're writing a `FooRepository` interface whose only implementation returns hardcoded/in-memory values with no real backing store, you're speculatively building the abstraction Phase 1's plan explicitly avoided.

2. **Putting a feature-specific `SettingsKey` into `AppSettingsKeys`.** `core.storage.AppSettingsKeys` holds only app-wide keys, and the Phase 2 plan is explicit: *"Do not add more base keys speculatively — feature-specific keys belong to the feature that owns them... not to `AppSettingsKeys`."* `DemoRepositoryImpl`'s counter key (`demo_counter_count`) is defined as a private constant inside the repository implementation itself, not in the shared keys object. Follow that pattern for any new per-feature persisted value.

3. **Forcing a `Flow`-returning use case to implement the suspend `UseCase<P,R>` contract.** `ObserveDemoCountUseCase` is a plain class precisely because `UseCase<in P, R>`'s `suspend operator fun invoke(params: P): R` shape doesn't fit an ongoing stream. Don't wrap a `Flow` in a suspend function just to satisfy an interface that doesn't apply.

4. **Overriding `attachBaseContext` or hand-rolling ViewBinding inflate in a new `Activity` instead of extending `BaseActivity`.** `BaseActivity<VB>` centralizes responsive context wrapping and binding inflate so every screen gets it automatically and consistently. Locale is handled by AppCompat per-app locales and the manifest `autoStoreLocales` service, not custom Activity context wrapping. Re-introducing a hand-rolled override in a new feature undoes that consistency.

5. **Recreating manual ViewModel factories now that Hilt exists.** Use constructor injection, `@HiltViewModel`, `@AndroidEntryPoint`, and feature-local Hilt modules for bindings. A custom `ViewModelProvider.Factory` should be rare and justified.

6. **Adding a new custom button variant (`CardButton`, `LinearButton`, `ConstraintButton`, etc.) before a real screen needs that specific shape.** The Phase 5 plan explicitly scoped this codebase to `ButtonStyleDelegate` + one concrete variant (`FrameButton`) out of the reference project's 9, stating: *"Porting all 9 with zero real screens needing more than one shape would be speculative, unused code... Adding `LinearButton`/`CardButton`/etc. is a follow-up whenever a real screen needs that specific shape — do not add them speculatively now."*
