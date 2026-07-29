# MODERNIZATION.md

Rolling plan for hardening `:core` into a genuinely consumable, modern Android library.
Written 2026-07-29. Each phase lands independently; this file is updated as phases complete,
not rewritten at the end.

**How to use this doc:** read the Baseline section before proposing any "modernization" work.
It records what is *already* correct, with evidence, specifically so that later sessions don't
burn effort re-fixing non-problems or reintroducing patterns that were deliberately rejected.
Decisions are at the bottom with their rationale — treat them as settled unless the stated
reasoning no longer holds.

## What triggered this, and why that is not the scope

The trigger was comparing `:core` against `com.kalapa.faceotphost.core` in
`/Users/thanhng224/AndroidStudioProjects/KalapaFaceOTPHost` — an in-app fork of an earlier
version of this base, and the first real prospective consumer.

That comparison is a *bad* lens for modernization, because it only surfaces what one particular
app happened to need. It found 8 integration blockers but missed the three largest real gaps
(window insets, predictive back, the responsive-sizing strategy) because that app never
exercised them.

So the scope here is **`:core` measured against the Android 2026 platform baseline and against
what a published library owes its consumers** — not against Kalapa's diff. Kalapa reappears
only at the end, as an integration test that proves the API is actually usable from outside.

## Baseline: what is already modern (do not "fix" these)

Verified against source on 2026-07-29. `:core` is **not** carrying legacy debt:

- **Deprecated API usage is effectively zero.** The single `@Suppress("DEPRECATION")` in the
  module, `core/navigation/ActivityNavigator.kt:75`, is a correct API-level fallback:
  `overrideActivityTransition()` on API 34+, `overridePendingTransition()` below. That is
  textbook compat, not debt. `navigation/ArgumentDelegates.kt:140` goes through `BundleCompat`,
  also correct.
- `ui/responsive/ResponsiveContextWrapper` uses `createConfigurationContext()`, **not** the
  deprecated `Resources.updateConfiguration()`.
- `androidx.security:security-crypto`'s deprecated `EncryptedSharedPreferences` was already
  replaced by a Keystore-resident AES-256/GCM key in `storage/secure/EncryptedSecureStore.kt`.
- `architecture/StateViewModel.kt` is a correct modern MVI shape: `StateFlow` for state,
  `Channel(BUFFERED).receiveAsFlow()` for one-shot effects. Nothing to change.
- Dependencies are current: AGP 9.3.1, Kotlin 2.4.10, Retrofit 3.0.0, OkHttp 5.4.0, Room 2.8.4,
  Material 1.14.0, coroutines 1.11.0.

**Consequence:** this effort is not "replace old APIs". It is *add what is correctly missing*
(insets, predictive back), *remove what is actively wrong* (overlapping responsive mechanisms),
and *make the library's contract visible* (public API surface). Do not expect to find many more
deprecated APIs to swap — there aren't any.

## Findings

### F1 — No public API contract (Phase 0)

`:core` is published to JitPack but has no `explicitApi()`, no binary-compatibility-validator,
and no committed API dump. The consequence is that its public surface is accidental: impls are
marked `internal` on good instinct, but nobody can see the resulting contract, and it can break
between versions silently.

**`explicitApi()` is enabled as of Phase 0** (`core/build.gradle.kts`), covering `src/main` and
`src/testFixtures` — the fixtures are published for consumers via
`testImplementation(testFixtures(...))`, so they are API too. It flagged 222 sites across 56
files (202 missing visibility, 20 missing return types), all now explicit.

**binary-compatibility-validator does not work here — do not retry it as configured.** BCV 0.18.0
registers no `apiDump`/`apiCheck` tasks for a `com.android.library` module, whether applied to
`:core` or to the root project with `apiValidation { }`. It binds to Kotlin JVM/multiplatform
source sets and skips AGP variants entirely; this is a BCV limitation, not an AGP 9.x
incompatibility, so waiting for a new AGP will not fix it. The plugin was added, tested, and
reverted rather than left half-wired.

Consequence: **there is currently no enforced API dump.** `explicitApi()` makes the surface
visible and reviewable in source, which is most of the value, but nothing mechanically fails a
build when the public API changes. The candidate replacement is **metalava** (what AndroidX
itself uses; it does support Android library variants, e.g. via the
`me.tylerbwong.gradle.metalava` plugin). Evaluate it before the v3.0.0 freeze — that freeze is
the point where an unenforced contract starts costing something.

Evidence of the real cost: a prospective consumer needed 11 declarations that are `internal`
(`DefaultAppDispatchers`, `DataStoreSettingsStore`, `appSettingsDataStore`, `RetrofitApiClient`,
`AuthTokenInterceptor`, `ConnectivityInterceptor`, `AndroidConnectivityChecker`,
`OkHttpFileTransferClient`, `EncryptedSecureStore`, `AndroidElapsedRealtimeClock`,
`AndroidStringProvider`). Most of those are only reachable because that app hand-wires DI; if
`:core`'s own Hilt modules bind every interface, a consumer never touches an impl. That
hypothesis is what Phase 0 verifies.

**Audit result (Phase 0, 2026-07-29): the hypothesis holds — no widening was needed.** Every
`internal` impl a consumer reached for sits behind a public interface with a binding in an
`@InstallIn(SingletonComponent::class)` module inside `:core`, so injecting the interface is the
supported path:

| Internal impl | Public interface | Binding |
| --- | --- | --- |
| `DefaultAppDispatchers` | `AppDispatchers` | `di/AppCoreModule.kt:35` |
| `EncryptedSecureStore` | `SecureStore` | `di/AppCoreModule.kt:39` |
| `SecureStoreAuthTokenProvider` | `AuthTokenProvider` | `di/AppCoreModule.kt:43` |
| `AndroidElapsedRealtimeClock` | `ElapsedRealtimeClock` (`fun interface`) | `di/AppCoreModule.kt:47` |
| `AndroidStringProvider` | `StringProvider` | `di/AppCoreModule.kt:51` |
| `DataStoreSettingsStore` | `SettingsStore` | `di/AppCoreModule.kt:62` |
| `RetrofitApiClient` | `ApiClient` | `di/NetworkModule.kt:32` |
| `OkHttpFileTransferClient` | `FileTransferClient` | `di/NetworkModule.kt:36` |
| `AndroidConnectivityChecker` | `ConnectivityChecker` | `di/NetworkModule.kt:50` |
| `AndroidThemeManager` | `ThemeManager` | `ui/theme/ThemeModule.kt:14` |
| `appSettingsDataStore` | — | Implementation detail of `provideSettingsStore`; not consumer-facing |
| `AuthTokenInterceptor`, `ConnectivityInterceptor` | — | Assembled inside `NetworkModule.provideOkHttpClient`; not consumer-facing |

`:core`'s DI modules are themselves `internal`, which is correct: Hilt aggregates `@InstallIn`
modules across the whole classpath including AAR dependencies, so a consumer never references a
module by name. The one requirement this places on consumers is that they apply the Hilt Gradle
plugin themselves — `README.md` must keep saying so.

### F2 — Documentation asserts API that does not exist (Phase 0)

`docs/CORE_MODULES.md:19-21` is wrong in three places, despite that file being named as source
of truth in `CLAUDE.md` and despite its own header claiming it was verified against source:

| Doc claims | Reality |
| --- | --- |
| `DomainResult` has a `map` extension | Absent |
| `AppError` has `Business(code, message)` | Absent |
| `ResultState.Error(message: String, ...)` | Actually `message: UiText` |

`map` and `Business` both exist in the older in-app fork, so they were lost during the `:core`
extraction rather than never written. This is a regression against documented behaviour, not a
new feature — which is why it is fixed in Phase 0 rather than treated as a consumer request.

Docs that confidently assert false API are worse than absent docs, because every downstream
decision built on them inherits the error. Treat CORE_MODULES.md claims as unverified until
checked against source.

### F3 — No window-inset handling (Phase 1)

`targetSdk 37`, and Android 15+ enforces edge-to-edge. `ui/window/WindowExtensions.kt` provides
`setImmersiveMode()`, which *hides* the system bars — a different concern. For an ordinary,
non-immersive screen there is no inset handling anywhere in the repo: no `enableEdgeToEdge()`,
no `ViewCompat.setOnApplyWindowInsetsListener`, no `fitsSystemWindows`, and no
`windowOptOutEdgeToEdge` either way.

Every consuming app therefore renders content under the status bar by default. This is a
correctness bug against the platform contract, not a preference.

### F4 — No predictive back (Phase 1)

`android:enableOnBackInvokedCallback` appears nowhere, and there is no `OnBackPressedCallback`
usage. Notable because `:core` ships `ui/base/TransitionActivity` and custom
`overrideActivityTransition` animations — back-navigation animation is clearly something this
base cares about, yet the modern back mechanism is entirely absent.

### F5 — Two overlapping responsive mechanisms, one of them a dated hack (Phase 2)

`:core` depends on `com.intuit.sdp`/`com.intuit.ssp` 1.1.1, which work by generating hundreds of
`@dimen/_Nsdp` buckets — a pre-`WindowSizeClass` workaround. On top of that,
`ui/responsive/ResponsiveContextWrapper` clamps `Configuration.smallestScreenWidthDp` into
`[320, 480]` and hands back a `createConfigurationContext()`.

These fight each other: clamping `smallestScreenWidthDp` **also changes which resource
qualifiers resolve**, so `values-sw600dp/` becomes unreachable on devices that should hit it.
The reason two mechanisms exist is that sdp/ssp alone doesn't produce good results, so a clamp
was layered on top. Removing both removes a layer rather than adding one.

Scale of the change: 14 layout files, 144 `@dimen/_Nsdp` references, a dedicated "sdp/ssp
convention" section in `docs/DESIGN_SYSTEM.md`, and three text styles
(`TextAppearance.AndroidXmlBase.BodyEmphasis`/`BodyMedium`/`Micro`) that exist *only* to
participate in the ssp convention. `ResponsiveContextWrapper` itself has exactly one call site,
`ui/base/BaseActivity.kt:30`.

Important: sdp/ssp and `WindowSizeClass` are **not** 1:1 replacements. sdp/ssp scales every
dimension continuously with screen width; `WindowSizeClass` is a Kotlin-side API for switching
layout and behaviour at discrete breakpoints, and cannot appear in XML at all. Replacing one
with the other means adopting a different philosophy — see D1.

### F6 — No Compose interop path (Phase 3)

The base is XML + ViewBinding by design and that premise is not in question. But there is no
Compose dependency and no `ComposeView` seam, so a consuming app cannot write a new screen in
Compose even incrementally. That blocks the normal evolution path for any codebase adopting
this base today.

### F7 — Library forces heavy dependencies and unconditional startup work on all consumers (Phase 4)

`core/src/main/AndroidManifest.xml` auto-registers four `androidx.startup` initializers via
manifest merge. One of them, `startup/DbPassphraseWarmupInitializer`, unconditionally derives or
reads a SQLCipher passphrase from the Keystore on every app start — including for consumers that
never touch Room. It only declares a dependency on `TimberInitializer`, with no opt-in gate.

Separately, `:core` pulls Room + SQLCipher (native `.so`, so real APK size), WorkManager, and
Lottie into every consumer regardless of use.

The manifest also merges in `AppLocalesMetadataHolderService` with `autoStoreLocales=true`,
which is a per-app-locale storage decision imposed on consumers — relevant to F8.

### F8 — Locale switching: unresolved, and possibly correct as-is (Phase 5)

`localization/LocaleManager` uses `AppCompatDelegate.setApplicationLocales()`. That is the
platform-sanctioned per-app locale: it persists across process death and appears in system
Settings → App → Language on API 33+. Its cost is that changing language recreates activities,
which flashes.

The older in-app fork rejected that and instead kept an in-memory `StateFlow` of the language
tag, called the deprecated `Resources.updateConfiguration()`, and re-inflated the live Activity
in place. That avoids the flash but loses persistence and system-Settings integration, and its
own source comments document two on-device crashes it had to work around (swapping views from
inside a click listener during touch dispatch, fixed with `post()`; and Material widgets failing
to resolve `?attr/` references, fixed with `cloneInContext`).

**Do not treat that fork's mechanism as the modern answer.** Those crash workarounds are
evidence that re-inflating a live Activity is fragile, not evidence that it is good. The likely
correct outcome is to keep `AppCompatDelegate` as the source of truth and address the flash
separately — or to accept the flash. Phase 5 is a spike whose valid outcome includes "change
nothing".

What must be preserved either way is the *knowledge*: if any future work re-inflates a live
Activity, those two crash modes are real and must be covered by tests.

## Phases

Each phase is independently landable and gated on `./gradlew check` staying green (which
includes Kover ≥80% line coverage on the unit-testable surface).

**Phase 0 is complete (2026-07-29).** `explicitApi()` is on for `src/main` and `src/testFixtures`
(222 sites fixed across 56 files); `DomainResult.map` and `AppError.Business` are restored with
tests; the internal/public audit found no widening was needed; `docs/CORE_MODULES.md` is
reconciled. `./gradlew check` green. One goal was **not** met: there is no enforced API dump,
because BCV cannot see Android library variants — see F1, and task "Evaluate metalava" before the
v3.0.0 freeze.

Adding `AppError.Business` made an exhaustive `when` in `:app`'s `DemoViewModel.toWeatherError()`
non-exhaustive, which is exactly the signal wanted: the in-repo consumer catches sealed-hierarchy
changes at compile time. Expect the same from any consumer branching on `AppError`.

| Phase | Work | Gate |
| --- | --- | --- |
| **0** | F1 + F2 — public API contract; restore documented-but-missing API | `explicitApi()` green; internal/public audit recorded. **Enforced API dump deferred — BCV unusable, see F1** |
| **1** | F3 + F4 — edge-to-edge insets, predictive back, R8 posture | Instrumented test on a device with visible system bars |
| **2** | F5 — retire sdp/ssp and `ResponsiveContextWrapper`; spacing scale + qualifiers + `WindowSizeClass` | `DESIGN_SYSTEM.md` and `CLAUDE.md` updated in the same commit |
| **3** | F6 — `ComposeView` interop + XML-theme→`MaterialTheme` bridge | A sample screen in `:app` rendering Compose inside an XML layout |
| **4** | F7 — opt-in initializers; decide module topology **from measurement** | Before/after APK size and startup trace of an empty consumer |
| **5** | F8 — locale spike; valid outcome includes "no change" | Written finding in this file either way |
| — | **Pick an API-tracking tool (metalava), freeze the contract, publish v3.0.0** | |
| **6** | Kalapa adopts the published AAR — the first real integration test | Kalapa builds and runs against the artifact, no workarounds |

Phase 1 precedes Phase 5 deliberately: insets and predictive back are platform-correctness
issues on `targetSdk 37`, whereas the locale flash is cosmetic and its right answer is unknown.

Phase 4 begins with measurement, not with a module split. If an empty consumer's APK size and
startup trace don't move meaningfully, the outcome is "make `DbPassphraseWarmupInitializer`
opt-in and keep one module" — which is also what `CLAUDE.md` prefers.

Phase 6 is where the API contract gets tested for real. Any workaround Kalapa needs is a `:core`
defect: file it against the base rather than patching it in the app, or the library silently
reverts to being an in-app package that happens to be published.

## Decisions

**D1 — Retire sdp/ssp in favour of a fixed spacing scale + resource qualifiers +
`WindowSizeClass`.** *(2026-07-29)*

Target model: named fixed values (`@dimen/space_16`, not `@dimen/_16sdp`) for the common case;
`values-sw600dp/dimens.xml` for the few dimensions that genuinely must differ; `WindowSizeClass`
in Kotlin for structural layout changes. `ResponsiveContextWrapper` is deleted, because clamping
`smallestScreenWidthDp` actively breaks the qualifier mechanism this model depends on (F5).

Rationale: it's the Google-recommended model, it drops a third-party dependency, layouts become
readable, and it removes an entire overlapping layer. Cost is real and accepted: 144 references
across 14 layouts, a `DESIGN_SYSTEM.md` rewrite, three text styles to reconsider, and a
`CLAUDE.md` rule to change (it currently commits to spreading the sdp/ssp convention across all
layouts — that commitment is withdrawn).

**D2 — Open a Compose interop path; XML stays the default.** *(2026-07-29)*

Minimum viable: `ComposeView` usable from `:core`'s base classes, plus a bridge from the XML
theme to `MaterialTheme` (natural home: the existing `ui/theme/AppTheme` + `ThemeManager`).
XML + ViewBinding remains the default and documented path. Compose is a heavy dependency, so its
packaging is decided together with F7's topology measurement in Phase 4, not separately.

**D3 — Break API freely until v3.0.0.** *(2026-07-29)*

No consumer exists yet — not even Kalapa, which still runs its own copied fork. So Phases 0–5
ship no deprecated aliases and no compatibility shims. `core/api/core.api` is a review tool
during those phases and becomes a binding contract only at the v3.0.0 freeze.

## Explicitly out of scope

- **Migrating off XML + ViewBinding to Compose-first.** That is the project's founding premise
  (`CLAUDE.md`); D2 opens interop, it does not start a migration.
- **Adding `MaxHeightNestedScrollView`** or other widgets that only one consuming app needs.
  Considered and rejected under YAGNI: it came from Kalapa's specific layout needs, not from a
  proven need in the base.
- **A `SecureStore` legacy migration helper** reading the old `EncryptedSharedPreferences`
  store. Only needed if an app with released installs adopts `:core`; add it when that is real,
  not before.
- **Speculative `DomainResult` combinators** (`flatMap`, `mapError`, …). `map` is restored in
  Phase 0 because docs already promise it and usage proves it; the rest wait for a real call
  site.
