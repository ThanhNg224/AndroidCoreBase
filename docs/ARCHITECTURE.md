# ARCHITECTURE.md

## Purpose  
This document defines the architectural principles for Android projects using Clean Architecture and MVVM. It focuses on maintainability, scalability, modularity, and separation of concerns to ensure robust and adaptable applications.

## Architecture Goals  
- Maintainability  
- Scalability  
- Testability  
- Predictability  
- Low coupling  
- High cohesion  

## High Level Architecture  
The architecture is divided into three layers:  
**Presentation**  
↓  
**Domain**  
↓  
**Data**  

Dependencies always point inward, from Presentation to Domain to Data.

## Layer Responsibilities  

### Presentation  
Responsibilities:  
- Render UI  
- Observe state  
- Handle user interaction  
- Navigation  

Must not:  
- Access APIs  
- Access database  
- Contain business rules  

### Domain  
Responsibilities:  
- Business rules  
- UseCases  
- Entities  
- Repository interfaces  

Must not:  
- Depend on Android framework  
- Know networking/database details  

### Data  
Responsibilities:  
- Repository implementations  
- Remote data  
- Local data  
- Caching  
- Data synchronization  

Must not:  
- Contain UI logic  
- Leak API or database models to Presentation  

## Dependency Rule  
Dependencies always flow from Presentation → Domain → Data. Reverse dependencies are strictly prohibited to maintain clear separation and independence.

## Dependency Matrix  
| From / To         | Presentation | Domain | Data | Feature | Shared/Core |
|-------------------|--------------|--------|------|---------|-------------|
| Presentation      | -            | ✅     | ❌   | ❌      | ✅          |
| Domain            | ❌           | -      | ❌   | ❌      | ✅          |
| Data              | ❌           | ✅*    | -    | ❌      | ✅          |
| Feature           | ❌           | ❌     | ❌   | ❌      | ✅          |
| Shared/Core       | ❌           | ❌     | ❌   | ❌      | -           |

*Data → Domain dependency only allowed for Repository interface usage.

## Feature, Screen, and Flow

A **feature** is a vertical slice that owns one user-facing capability or bounded business context, such as authentication, profile, or checkout. It owns the presentation, domain, and data code required for that capability. A feature is not a synonym for one Activity, Fragment, or layout.

A **screen** is one concrete presentation destination within a feature: an Activity, Fragment, dialog destination, or equivalent navigable UI state. A screen owns its ViewModel and its `UiState`/`UiEvent`/`UiEffect`, because those types describe that screen's presentation contract.

A **flow** is a user journey through one or more screens, for example Login → OTP verification → password reset. A flow does not introduce another package layer. Its screens remain owned by their feature; when a journey crosses feature boundaries, communicate through navigation and stable public contracts rather than direct feature dependencies.

The app uses a single **app shell** for its equally important top-level areas. `MainActivity` owns the shared app bar, `BottomNavigationView`, and `NavHostFragment`; Home, Demo, and UI Kit are Fragment destinations in `main_navigation.xml`. A bottom-navigation item represents a top-level destination, not a feature package. Secondary utilities such as Settings are opened from the app bar and may use a separate Activity when they form a self-contained task. Do not add Settings, dialogs, or every screen in a feature to the bottom bar.

Each feature should own:

- Its screens and feature-specific presentation code.
- Its UseCases and repository interface usage.
- Its data implementation when real persistence or remote data is needed.

Avoid coupling between features to promote modularity and independent development. Do not create a root-level `screens/` package: it separates a screen from the capability, state, and use cases it belongs to, which makes ownership less clear.

## Recommended Feature Structure

For a feature with one screen, keep the presentation packages flat. `sample/demo` uses this shape and avoids speculative nesting; product capabilities belong under `feature/<feature-name>/`.

```
feature/<feature-name>/
    presentation/
        ui/
        viewmodel/
        state/
    domain/
        model/
        repository/
        usecase/
    data/
        datasource/
        repository/
        mapper/
```

When a feature grows to two or more screens, group the presentation code by screen while keeping domain and data at the feature level:

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
        components/                 # only when shared by two or more auth screens
    domain/
        repository/
        usecase/
    data/
        datasource/
        repository/
        mapper/
```

Do not create empty `domain/`, `data/`, `components/`, or per-screen packages merely to match this example. If a second screen has a genuinely separate business capability and does not share domain/data ownership, model it as a separate feature instead. The exact shape may vary, but ownership and dependency rules must remain consistent.

## Repository Pattern  
Repositories coordinate multiple data sources to provide a unified interface. Repository interfaces belong to the Domain layer, while their implementations reside in the Data layer.

## Data Sources  
Data sources include:  
- RemoteDataSource  
- LocalDataSource  
- Optional CacheDataSource  

Repositories orchestrate these data sources to provide consistent data.

## UseCase Guidelines  
- One business capability per UseCase  
- Composable and reusable  
- Independent of framework and UI  
- Easily testable  

## Mapper Guidelines  
Mapping flow:  
- ApiModel → Entity → UiModel  
- DatabaseModel → Entity  

Mappers should be deterministic and maintain clear transformations between layers.

## MVVM Responsibilities  
**View:**  
- Render UI only  

**ViewModel:**  
- Own screen state  
- Coordinate UseCases  
- Contain no Android framework business logic  

## Data Flow  
Recommended request flow:  
User Action → View → ViewModel → UseCase → Repository → DataSource → API/Database.  
The response flows back in reverse order after appropriate mapping at each layer, ensuring data consistency and separation of concerns.

## UI State  
Use a clear separation of:  
- UiState (screen state)  
- UiEvent (user or system events)  
- UiEffect (one-time effects like navigation or messages)  

## Dependency Injection  
- Use constructor injection  
- Inject abstractions, not implementations  
- Keep modules independent and loosely coupled  

## Shared Modules  
- Move code into shared/core modules only after proven reuse  
- Avoid feature-specific logic in shared modules to maintain modularity  

## Shared Module Rules  
- Shared code must be framework-agnostic where possible  
- Never depend on feature modules  
- Avoid business logic tied to a single feature  
- Prefer moving code only after two or more proven reuse cases  

## Feature Communication  
- Communicate through public contracts/interfaces  
- Avoid direct dependencies between features  

## Error Propagation  
- Errors propagate from Data → Domain → Presentation  
- Convert technical errors into domain-friendly results  
- UI presents user-friendly error messages  

## Architecture Smells  
- Business logic in UI  
- ViewModel accessing APIs directly  
- Domain layer depending on Android framework  
- Circular dependencies between modules  
- God repositories handling too many responsibilities  
- Massive ViewModels  
- Duplicate business logic scattered across layers  
- Shared modules depending on feature-specific code  

## Refactoring Strategy  
Improve architecture incrementally by making small, safe refactors that preserve existing behavior. Avoid big-bang rewrites to reduce risk and maintain project stability. Prioritize continuous improvement and maintainability through gradual enhancements.

## Architecture Principles  
- **Separation of Concerns:** Each layer and module has a distinct responsibility to reduce complexity.  
- **Dependency Inversion:** High-level modules should not depend on low-level modules; both depend on abstractions.  
- **Single Responsibility:** Classes and modules should have one reason to change, focusing on a single task.  
- **Feature Isolation:** Features should be self-contained to enable independent development and testing.  
- **Composition over Inheritance:** Prefer composing behaviors over complex inheritance hierarchies for flexibility.  
- **Explicit Dependencies:** Dependencies should be clearly declared and injected to improve testability and clarity.  
- **Predictable Data Flow:** Data should flow in a clear, unidirectional manner to simplify reasoning and debugging.  
- **Stable Public Contracts:** Interfaces between modules should be stable and well-defined to minimize coupling.  

## Architecture Review Checklist  
- [ ] Are layer boundaries clearly defined and respected?  
- [ ] Do dependencies flow inward only (Presentation → Domain → Data)?  
- [ ] Is business logic contained exclusively in the Domain layer?  
- [ ] Are UI components free of business and data access logic?  
- [ ] Are repository interfaces defined in Domain and implementations in Data?  
- [ ] Are UseCases focused on a single business capability?  
- [ ] Are mappers deterministic and correctly transforming data between layers?  
- [ ] Is dependency injection used consistently with abstractions?  
- [ ] Are shared modules free from feature-specific logic?  
- [ ] Is feature communication handled via public contracts without tight coupling?  
- [ ] Is error handling consistent and user-friendly across layers?  
- [ ] Are ViewModels free from Android framework dependencies and business logic?  
- [ ] Is UI state management separated into UiState, UiEvent, and UiEffect?  
- [ ] Are data sources properly encapsulated and orchestrated by repositories?  
- [ ] Are modules designed for low coupling and high cohesion?  
- [ ] Are there no circular dependencies between modules or layers?  
- [ ] Is the architecture scalable and maintainable for future growth?  
- [ ] Are tests easily written for UseCases, repositories, and ViewModels?

## Current Package Layout (Phase 0-6)

Everything above this section describes the target architecture. The folders below are what actually exists in the codebase today; check here (or the source tree) before assuming a core module already exists.

See `docs/FEATURE_TEMPLATE.md`, `docs/CORE_MODULES.md`, and `docs/DESIGN_SYSTEM.md` for a full walkthrough of building a new feature on top of the layout below.

`feature/settings` is the first product vertical slice. It owns app-preference presentation and adapts the app-wide theme and locale services through its own repository contract. `sample/` remains reference code only.

app/src/main/java/com/example/androidxmlbase/
  MainActivity.kt                            # app shell: app bar + NavHostFragment + bottom navigation
  appshell/
    home/HomeFragment.kt                     # shell-owned landing destination; no business layer
  core/
    architecture/
      UiState.kt
      UiEvent.kt
      UiEffect.kt
      AppDispatchers.kt
      UseCase.kt
      StateViewModel.kt
      result/
        ResultState.kt
        DomainResult.kt                      # also declares AppError & map helper
    storage/
      settings/
        SettingsKey.kt                       # typed key sealed class (String/Int/Long/Boolean/Float)
        SettingsStore.kt                     # observe/get/set/remove contract
        DataStoreSettingsStore.kt            # DataStore<Preferences>-backed implementation
        AppDataStore.kt                      # Context.appSettingsDataStore delegate
        AppSettingsKeys.kt                   # app-wide keys only; feature keys stay feature-private
      secure/
        SecureStore.kt                       # + SecureStoreKey, SecureStoreKeys
        EncryptedSecureStore.kt
    network/
      ApiResult.kt                           # Success/HttpError/NetworkError/ParseError/EmptyBody
      ApiConfig.kt
      ApiClient.kt
      RetrofitApiClient.kt                   # classifies Retrofit calls into ApiResult
      NetworkClientFactory.kt                # OkHttp timeouts + Retrofit composition root
      auth/
        AuthTokenProvider.kt                 # + NoOpAuthTokenProvider
        SecureStoreAuthTokenProvider.kt
        AuthTokenInterceptor.kt
      connectivity/
        ConnectivityChecker.kt               # + AndroidConnectivityChecker
        ConnectivityInterceptor.kt
      transfer/
        FileTransferClient.kt                # + OkHttpFileTransferClient
        TransferResult.kt
        ProgressRequestBody.kt
    localization/
      AppLanguage.kt                         # AppLanguage registry: English (en) and Vietnamese (vi-VN)
      LocaleManager.kt                       # reads/applies AppCompat app locales through a unit-testable applier
    ui/
      text/
        StringProvider.kt                    # lets a ViewModel resolve string resources without an Android Context
        AndroidStringProvider.kt              # real Context-backed implementation
      base/
        BaseActivity.kt                      # ViewBinding inflate + responsive attachBaseContext + collectOnStarted + immersive window cutout support
        BaseFragment.kt
        BaseDialogFragment.kt
        BaseBottomSheetDialogFragment.kt
        LifecycleFlowExtensions.kt            # shared collectOnStartedBy(lifecycleOwner) used by all Base* hosts
        ResultStateOverlay.kt                 # shared full-screen-loader + error-prompt rendering used by BaseActivity/BaseFragment.bindResultState
        Debouncer.kt                          # pure shouldAllow(nowMs) rate limiter + View.setOnDebouncedClickListener glue
        ResultRenderState.kt                  # ResultState<T>.toRenderState() (loading/content/error visibility + error message) + View.applyVisibilityTo glue
      responsive/
        ResponsiveConfig.kt                  # enabled + min/max smallestScreenWidthDp
        ResponsiveContextWrapper.kt          # clamps Configuration.smallestScreenWidthDp into the configured range
      drawable/
        ShapeDrawableFactory.kt              # builds runtime GradientDrawables (RECTANGLE/OVAL) shared by the components below
      window/
        WindowExtensions.kt                  # edge-to-edge window configuration
      components/
        ButtonStyleDelegate.kt               # shared shape/ripple background logic, framework-attribute-agnostic
        FrameButton.kt                       # FrameLayout-based button, the one ButtonStyleDelegate consumer ported so far
        ShadowLayout.kt                      # FrameLayout drawing an elevation+outline shadow (caller sets android:elevation)
        ThemedSwitch.kt                      # MaterialSwitch wrapper tinted from the color tokens
        StyledSnackbar.kt                    # Snackbar message surface styled with the color tokens
        FullScreenLoaderView.kt              # Custom full-screen loading spinner overlay
        PromptDialogFragment.kt              # Status prompt dialog fragment (success, fail, info) with action callbacks
    navigation/
      ArgumentDelegates.kt                   # type-safe Activity extras and Fragment argument accessors
      ActivityDestination.kt
      ActivityNavigator.kt                   # transition support (SLIDE_HORIZONTAL, FADE, etc.)
      NavigationOptions.kt
    time/
      ElapsedRealtimeClock.kt                # Monotonic elapsed time utility (clock)
    di/
      AppCoreModule.kt, NetworkModule.kt  # Hilt app/core wiring
  feature/
    settings/
      domain/
        repository/SettingsRepository.kt
        usecase/ObserveThemeUseCase.kt, SetThemeUseCase.kt, GetCurrentLanguageUseCase.kt, SetLanguageUseCase.kt
      data/
        repository/SettingsRepositoryImpl.kt # adapts ThemeManager and LocaleManager
      presentation/
        state/                               # SettingsUiState, SettingsUiEvent, SettingsUiEffect
        viewmodel/                           # SettingsViewModel
        ui/                                  # SettingsActivity + LanguageTransitionAction
      di/SettingsModule.kt
  sample/
    demo/
      domain/
        repository/DemoRepository.kt
        usecase/IncrementCounterUseCase.kt, ObserveDemoCountUseCase.kt, SaveDemoCountUseCase.kt,
          FetchDemoWeatherUseCase.kt
      data/
        repository/DemoRepositoryImpl.kt     # SettingsStore- and remote-data-source-backed
        dto/DemoMessageDto.kt
        datasource/DemoApiService.kt, DemoRemoteDataSource.kt (+ DemoRemoteDataSourceImpl)
        mapper/DemoMessageMapper.kt          # ApiResult<DemoMessageDto> -> DomainResult<String>
      presentation/
        state/DemoUiState.kt, DemoUiEvent.kt, DemoUiEffect.kt, DemoWeatherState.kt
        viewmodel/DemoViewModel.kt
        ui/DemoFragment.kt
      di/DemoModule.kt
    designsystem/
      presentation/
        state/DesignSystemUiState.kt, DesignSystemUiEvent.kt
        viewmodel/DesignSystemViewModel.kt   # StateViewModel<DesignSystemUiState, DesignSystemUiEvent, UiEffect>, synchronous setState, no data/domain layers
        ui/DesignSystemFragment.kt           # showcases FrameButton, ShadowLayout, ThemedSwitch, StyledSnackbar, and the ResultState demo

`feature/settings` is the canonical single-screen product feature. `SettingsActivity` renders theme and language as settings-list rows and uses single-choice dialogs for their finite values; those selections do not become separate screens merely to demonstrate package nesting. Its feature-owned `LanguageTransitionAction` runs inside the opaque core `TransitionActivity`, so the core owns only the reusable transition host. `SettingsRepository` is a feature-domain contract; its implementation adapts the reusable `ThemeManager` and `LocaleManager`, so UI never calls either core service directly. Hilt wires the feature binding in `feature/settings/di/SettingsModule`.

`sample/demo` remains the data/network reference: its counter persists through `DemoRepositoryImpl`, backed by the real `DataStoreSettingsStore`, and it fetches live weather for Ho Chi Minh City through `DemoRemoteDataSourceImpl` -> `DemoRepositoryImpl.fetchWeather()` -> `FetchDemoWeatherUseCase`. `DemoWeatherResponseDto` is mapped to the pure `DemoWeather` domain model before presentation sees it. Sample-specific Retrofit service providers stay in the sample package, while `core/di` only provides reusable Retrofit/OkHttp infrastructure. Product feature providers follow the same ownership rule under their own `feature/<name>/di` package. `SecureStore` handles auth/refresh tokens separately from normal settings, and backup/data-extraction rules exclude the secure store shared-preferences file.

`MainActivity` and `SettingsActivity` extend `core/ui/base/BaseActivity`; `HomeFragment`, `DemoFragment`, and `DesignSystemFragment` extend `BaseFragment`. Both base hosts own ViewBinding inflation and expose lifecycle-safe `collectOnStarted`. `MainActivity` is the composition root for global navigation and delegates destination content to Fragments. `DesignSystemFragment.render()` consumes `ResultState<T>.toRenderState()`, while `DemoFragment` maps `DemoWeatherState` to localized strings. Its increment and refresh controls use `View.setOnDebouncedClickListener` to avoid accidental duplicate actions.
