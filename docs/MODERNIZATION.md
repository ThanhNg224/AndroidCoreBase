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

Verified against source on 2026-07-29. `:core` is **not** carrying legacy debt — with one
exception found later, in XML rather than Kotlin: see F9 (`android:statusBarColor`). The scan
below covered `.kt` files; treat "no deprecated usage" as proven for Kotlin only.

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

### F4 — Predictive back: **not a real gap. This finding was overstated.**

Originally written as "predictive back is entirely absent" on the basis that
`android:enableOnBackInvokedCallback` appears nowhere. Verified 2026-07-29 and that conclusion was
wrong on both halves:

- `targetSdk = 37`. Predictive back is **on by default** for apps targeting API 36+, and the
  `enableOnBackInvokedCallback` manifest flag is ignored in that range. Adding it would be
  cargo cult.
- Nothing overrides the deprecated `Activity.onBackPressed()` anywhere in `:core` or `:app` — the
  one back-related call site,
  `feature/settings/.../SettingsActivity.kt:26`, uses
  `onBackPressedDispatcher.onBackPressed()`, which is the correct modern form.

So the base already gets predictive back, and nothing blocks it. The only remaining work is
**optional polish**: custom predictive-back animations via `OnBackAnimationCallback`, which would
pair with the `TransitionActivity`/`overrideActivityTransition` work `:core` already has. Not
scheduled — it is a nice-to-have, not a correctness issue.

### F9 — `android:statusBarColor` is deprecated and ignored on Android 15+ (Phase 1)

Found while implementing F3, and it is also a **correction to the Baseline section above**: the
"deprecated usage is effectively zero" claim came from grepping `.kt` files only. XML themes were
not checked, and they do contain a deprecated API.

`android:statusBarColor` is set in `core/src/main/res/values/themes.xml:37` and
`core/src/main/res/values-night/themes.xml:4`. It has no effect once an app targets SDK 35+, so on
`targetSdk 37` these two lines are dead. Worse, setting them encodes the assumption that the app
is *not* laid out edge-to-edge — which is the same wrong assumption F3 describes.

`android:windowLightStatusBar` in those same styles is **still honoured** (it controls status-bar
icon contrast) and stays.

### F10 — `:core`'s consumer ProGuard rules have never been executed (own phase)

`app/build.gradle.kts` disables R8 for `release` (`optimization { enable = false }`), yet `:core`
ships `core/consumer-rules.pro` specifically for consumers that *do* minify. Those rules —
including the `-keep` on `storage.database.**` that Room needs — have therefore never run in any
build in this repo. A consumer enabling R8 would be the first to discover whether they are
correct, and the failure mode is a runtime crash in a release build, which is the worst place to
find out.

Not folded into Phase 1: turning R8 on means validating serialization, Room codegen and reflection
against a real minified release build, which is its own scoped piece of work with its own
verification. Tracked separately.

### F11 — `com.intuit.sdp`'s 1:1 baseline is 300dp, not 360dp (found during Phase 2 verification)

Phase 2's task brief asserted `@dimen/_16sdp` resolves to ~16dp "at the ~360dp baseline width sdp
is calibrated to," so replacing `_Nsdp` with a literal fixed `Ndp` value would be a near-visual-noop
on an ordinary phone. **That premise is false for `com.intuit.sdp:sdp-android:1.1.1`, verified
directly against the library's own resources** (extracted from the Gradle cache transform, not
inferred):

| Bucket | `_16sdp` | `_24sdp` | `_72sdp` |
| --- | --- | --- | --- |
| `values-sw300dp` | 16.00dp | 24.00dp | 72.00dp |
| `values-sw360dp` | 19.20dp | 28.80dp | 86.40dp |

The library's true 1:1 calibration point is **300dp**, not 360dp — at 360dp (this repo's physical
verification device: 1080×2400px, density 480/xxhdpi, exactly 360dp smallestScreenWidthDp) every
sdp value was already being scaled up by the device's own factor of 360/300 = **1.2×** before this
migration. Screenshotting before/after on that device (see Phase 2 verification below) confirms
this concretely: `fragment_appshell_home.xml`'s greeting subtitle wraps to 2 lines before the
migration and fits on 1 line after, because `paddingHorizontal="@dimen/_24sdp"` (28.8dp before) is
measurably tighter than `core_space_24` (24dp, fixed) — a real ~17% reduction in every
previously-scaled dimension on this device class, not a token-mapping bug. All 148 references were
independently re-verified as a faithful 1:1 rename of the numeric suffix (see Phase 2 completion
note); the discrepancy is entirely in the premise, not the execution.

**Not fixed as part of Phase 2** — silently multiplying every new token by 1.2× (or by whatever
ratio makes an arbitrary device's old look match) would be a unilateral design change to the entire
spacing scale, not a mechanical migration, and the literal-numeric-suffix mapping was what Phase 2
was explicitly briefed to do. Whether the base's spacing/radius/size scale should be tightened
(current state), rescaled up to preserve the pre-Phase-2 density on ~360dp phones, or something
else is a product decision for whoever owns the visual design, not something to decide inside a
mechanical dependency-removal pass.

### F12 — `docs/DESIGN_SYSTEM.md`'s `FrameButton`/`ShadowLayout` XML examples reference
non-existent attributes (found during Phase 2 doc updates)

`core/src/main/res/values/attrs.xml` declares `coreButtonBackgroundColor`, `coreButtonCornerRadius`,
`coreButtonStrokeWidth`, `coreButtonStrokeColor`, `coreButtonShape`, `coreShadowCornerRadius`,
`coreShadowBackgroundColor` — every real layout (`fragment_demo.xml`, `fragment_design_system.xml`,
`core_dialog_prompt.xml`) uses these `core`-prefixed names. `docs/DESIGN_SYSTEM.md`'s `FrameButton`
and `ShadowLayout` code samples use `buttonBackgroundColor`, `buttonCornerRadius`, `buttonShape`,
`buttonStrokeColor`, `shadowCornerRadius` — none of which exist as declared attrs. This is the same
class of defect as F2 (docs asserting API that isn't real), just in a code sample rather than prose.

**Not fixed as part of Phase 2** — out of scope (Phase 2 only touched the dimension values inside
those samples, keeping the pre-existing attribute names as-is to avoid conflating an unrelated fix
with this migration). Fix by adding the `core` prefix to those 5 attribute names in
`docs/DESIGN_SYSTEM.md`.

### F13 — `artifactId` no longer matches the GitHub repo name, which breaks JitPack (blocks any publish)

The Phase 2.5 rebrand changed `core/build.gradle.kts`'s `artifactId` from `AndroidXmlBase` to
`AndroidCoreBase` and updated the POM urls and `README.md` coordinates, but the GitHub repo is
still `ThanhNg224/AndroidXmlBase` — `git remote` was never changed.

That regresses a bug already diagnosed and fixed once (see the `jitpack-core-publishing` project
memory, bug 3): because `:core` is the only module applying `maven-publish`, JitPack treats this as
a **single-artifact repo** and serves it *only* at `com.github.<user>:<repo>:TAG`. So `artifactId`
must equal the GitHub repo name.

Verified locally rather than assumed — `./gradlew :core:publishToMavenLocal` produces:

```
~/.m2/repository/com/github/ThanhNg224/AndroidCoreBase/v2.0.0   <- what we now publish
~/.m2/repository/com/github/ThanhNg224/AndroidXmlBase/<tag>      <- where JitPack looks
```

Consequences: the next tag pushed to JitPack resolves to "File not found", and `README.md:63/91/94`
already advertise `com.github.ThanhNg224:AndroidCoreBase:v2.0.0`, which resolves for nobody — the
repo is not renamed *and* tag `v2.0.0` predates the rebrand, so its published artifact is
`AndroidXmlBase`.

Chosen fix (2026-07-29): rename the GitHub repo to `AndroidCoreBase`, update `git remote`, and cut a
fresh tag — a new tag is required either way before README's coordinate is truthful, since no
existing tag contains a rebranded artifact. Note the version question this raises: the package
rename (`com.thanhng224.androidxmlbase.core` → `...androidcorebase.core`) is a breaking change for
any consumer, so it warrants a major bump, which collides with this file's plan to publish `v3.0.0`
at the post-Phase-5 API freeze. Decide whether the rebrand takes `v3.0.0` and the freeze becomes
`v4.0.0`, or the rebrand ships unpublished until the freeze.

Verify per the memory, don't guess: fetch `https://jitpack.io/com/github/<user>/<repo>/<tag>/build.log`.

### F14 — `BaseComposeActivity` is a published abstraction with no Compose in it (Phase 3)

Phase 2.5 added `core/ui/base/BaseComposeActivity.kt` as a stub "ready for Phase 3". As shipped it
contains **zero Compose code**, and `:core` has **no Compose dependency at all** — no BOM, no
`androidx.compose.*`, no `activity-compose`. The whole class body renames `onCreate` to
`onSetupComposeContent` and adds nothing over `BaseActivity`; a consumer could override `onCreate`
and call `setContent` with identical results.

Two problems. It is public API in a library published to JitPack, named as though it provides
Compose support while providing none — a consumer extending it gets no theme bridge and no
`ComposeView` support, and must supply every Compose dependency themselves. And it is the same
speculative-generality category that `WindowSizeClass` was correctly *deferred* for in Phase 2
(F5/D1), so the judgment is inconsistent: one unused abstraction was rightly held back, the other
shipped.

Not a defect in direction — D2 already decided to open a Compose interop path. It is premature
packaging. Phase 3 resolves it by giving the class real substance (Compose BOM, `ComposeView`
interop, XML-theme→`MaterialTheme` bridge) or by deleting it if D2's minimum turns out not to need a
dedicated Activity base at all. Do not leave it hollow in a published release.

### F5 — Two overlapping responsive mechanisms, one of them a dated hack (Phase 2)

`:core` depends on `com.intuit.sdp`/`com.intuit.ssp` 1.1.1, which work by generating hundreds of
`@dimen/_Nsdp` buckets — a pre-`WindowSizeClass` workaround. On top of that,
`ui/responsive/ResponsiveContextWrapper` clamps `Configuration.smallestScreenWidthDp` into
`[320, 480]` and hands back a `createConfigurationContext()`.

These fight each other: clamping `smallestScreenWidthDp` **also changes which resource
qualifiers resolve**, so `values-sw600dp/` becomes unreachable on devices that should hit it.
The reason two mechanisms exist is that sdp/ssp alone doesn't produce good results, so a clamp
was layered on top. Removing both removes a layer rather than adding one.

Scale of the change: 148 references (144 `_Nsdp` + 4 `_Nssp`) across 13 layout, drawable,
theme and text-style files, a dedicated "sdp/ssp
convention" section in `docs/DESIGN_SYSTEM.md`, and three text styles
(`TextAppearance.AndroidCoreBase.BodyEmphasis`/`BodyMedium`/`Micro`) that exist *only* to
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

**Phase 1 is complete (2026-07-29).** `BaseActivity` now calls `enableEdgeToEdge()` before
`setContentView` and pads the binding root via `applySystemBarInsetsAsPadding()`, opt-out through
`applyInsetsToRoot` and skipped under `useImmersiveMode`. Both dead `android:statusBarColor` items
are gone (F9). F4 was corrected to a non-finding, and R8 was split out as F10 rather than rushed.

The inset helper takes per-edge flags because "apply insets once per edge" is the actual rule, and
getting that wrong is visible: the first attempt padded the root's bottom edge while Material's
`BottomNavigationView` also padded itself, leaving a dead strip inside the nav card. Note that
`app:paddingBottomSystemWindowInsets="false"` does **not** suppress it on `BottomNavigationView` —
that was tried and had no effect. What works is replacing the view's own listener
(`MainActivity.onBindingReady`), since Material installs its inset handling as an
`OnApplyWindowInsetsListener` and setting one overwrites it.

Verification is a screenshot on a physical API 33 device, not a unit test: `enableEdgeToEdge()`
opts in on all API levels, so the behaviour reproduces below Android 15 even though that is where
it becomes mandatory. Content clears the status bar and the nav pill sits clear of the navigation
bar with no internal dead space.

**Phase 2 is complete (2026-07-29).** All 148 `_Nsdp`/`_Nssp` references (144 + 4, across the 13
files F5 identified) are migrated to fixed `core_space_<n>` / `core_radius_<n>` / `core_size_<n>` /
`core_stroke_width` / `core_text_size_<n>` tokens in `core/src/main/res/values/dimens.xml` — verified
mechanically (a script built a full attribute+value inventory from source, applied a reviewed 1:1
mapping, and a post-migration grep confirmed exactly 148 new-token references with zero `_Nsdp`/
`_Nssp` left in `app/src/main/res` or `core/src/main/res`). `ResponsiveContextWrapper`/
`ResponsiveConfig` are deleted along with `BaseActivity`'s `attachBaseContext` override and
`responsiveConfig` property (their one call site); `com.intuit.sdp`/`com.intuit.ssp` are removed from
`core/build.gradle.kts`, `app/build.gradle.kts` (which had its own direct dependency on both, not
previously noted), and `gradle/libs.versions.toml`. The 3 `BodyEmphasis`/`BodyMedium`/`Micro` text
styles are **kept** — `core_dialog_prompt.xml` actively uses all 3 — just repointed from `_Nssp` to
`core_text_size_<n>`. `./gradlew check` is green (unit tests, lint, ktlint, detekt, Kover ≥80%).

**`WindowSizeClass` is explicitly deferred, not added** — D1 named it as part of the target model,
but no screen in this base branches on a breakpoint today, and `CLAUDE.md`'s base-building exception
permits foundational infrastructure, not unused abstractions. Add `androidx.window` and the
breakpoint-switching helper when a real screen needs one, not speculatively.

**Verified by physical-device screenshot, not just `./gradlew check`** (device: 1080×2400px,
density 480 / xxhdpi = exactly 360dp smallestScreenWidthDp), Home/Demo/UI Kit/Settings, before and
after. Demo, UI Kit, and Settings are visually unchanged. **Home is not** — see F11: the premise that
this migration is a near-no-op at "baseline ~360dp" turned out to be false for
`com.intuit.sdp:sdp-android:1.1.1`, whose actual 1:1 calibration point is 300dp, so this exact
360dp device was already 1.2× scaled beforehand. That is a finding about the premise, not a defect
in the migration — the 148-reference mapping itself was independently verified as an exact,
faithful rename.

**Phase 2.5 is complete (2026-07-29).** The project and modules were rebranded from `AndroidXmlBase` to **`AndroidCoreBase`**:
- Root project renamed to `AndroidCoreBase` in `settings.gradle.kts`.
- Core package moved to `com.thanhng224.androidcorebase.core` across `src/main`, `src/test`, and `src/testFixtures`.
- App sample package moved to `com.example.androidcorebase` across `src/main`, `src/test`, and `src/androidTest`.
- Themes and text styles renamed to `Theme.AndroidCoreBase`, `Base.Theme.AndroidCoreBase`, and `TextAppearance.AndroidCoreBase.*`.
- Activity base hierarchy refactored: `BaseActivity` is now a neutral base class without ViewBinding parameters, managing edge-to-edge window insets, lifecycle flow collection (`collectOnStarted`), and MVI helpers; `BaseBindingActivity<VB : ViewBinding>` is introduced for XML ViewBinding activities; `BaseComposeActivity` stub is created ready for Phase 3 Compose Interop.

| Phase | Work | Gate |
| --- | --- | --- |
| **0** | F1 + F2 — public API contract; restore documented-but-missing API | `explicitApi()` green; internal/public audit recorded. **Enforced API dump deferred — BCV unusable, see F1** |
| **1** | F3 + F9 — edge-to-edge insets; drop dead `statusBarColor`. F4 turned out to be a non-finding; R8 split out as F10 | **Done 2026-07-29** — verified by screenshot on a physical API 33 device |
| **2** | F5 — retire sdp/ssp and `ResponsiveContextWrapper`; spacing scale + qualifiers | **Done 2026-07-29** — `DESIGN_SYSTEM.md` and `CLAUDE.md` updated in the same commit. `WindowSizeClass` deferred (no consumer yet); see F11/F12 for what the verification found |
| **2.5** | Rebrand & Neutralize Base Architecture (`AndroidCoreBase`) | **Done 2026-07-29** — Root project, packages, themes rebranded to `AndroidCoreBase`; `BaseActivity` refactored into neutral `BaseActivity` + `BaseBindingActivity` + `BaseComposeActivity` stub |
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
readable, and it removes an entire overlapping layer. Cost is real and accepted: 148 references
across 13 files, a `DESIGN_SYSTEM.md` rewrite, three text styles to reconsider, and a
`CLAUDE.md` rule to change (it currently commits to spreading the sdp/ssp convention across all
layouts — that commitment is withdrawn).

**Executed 2026-07-29 (Phase 2), with one correction to this decision's own premise:** the fixed
values use each `_Nsdp`/`_Nssp`'s literal numeric suffix as the new `Ndp`/`Nsp` constant — a
same-number rename, not a rescale. That was believed to be close to a visual no-op on an ordinary
phone; verification found that belief was wrong (see finding F11) — `com.intuit.sdp` 1.1.1's true
1:1 point is 300dp, not ~360dp, so this rename is a real, measurable, roughly-17%-tighter spacing
scale on a typical 360dp-wide phone versus what sdp/ssp actually rendered. The decision to retire
sdp/ssp and `ResponsiveContextWrapper` stands regardless — that rationale (dropping a third-party
dependency, removing a mechanism that broke `values-sw600dp/` resolution, readable layouts) doesn't
depend on the calibration-baseline claim. Whether the resulting tighter density is desirable, or
the scale should be multiplied up to match the old rendered sizes, is an open product decision —
see F11.

The `WindowSizeClass` half of this decision is **deferred, not implemented**: no screen in this
base branches on a breakpoint today, so adding the dependency now would be speculative generality
that `CLAUDE.md`'s base-building exception does not cover. Add it when a real screen needs
breakpoint-based layout switching.

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
