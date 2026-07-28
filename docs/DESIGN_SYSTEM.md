# DESIGN_SYSTEM.md

The design tokens, custom components, and dimension convention this base ships today. `sample/designsystem`'s `DesignSystemFragment` (`app/src/main/res/layout/fragment_design_system.xml`) is the live, working reference for everything in this document — when in doubt about how a component is actually used, read that layout and its `DesignSystemViewModel`/`DesignSystemFragment` first. It is a developer-facing reference showcase, not a model for a business feature's domain/data shape.

## Color tokens

Defined in `app/src/main/res/values/colors.xml`:

| Token | Value | Intended usage |
|---|---|---|
| `color_primary` | `#FF3F51B5` | Primary brand color — filled button backgrounds, checked-switch tint, outlined-button stroke/text. |
| `color_on_primary` | `#FFFFFFFF` | Text/icon color on top of `color_primary` (e.g. filled button label). |
| `color_surface` | `#FFFFFFFF` | Card/sheet/toast background. |
| `color_on_surface` | `#FF1A1A1A` | Text/icon color on top of `color_surface` (also the unchecked-switch tint). |
| `color_error` | `#FFB00020` | Reserved for error-state UI. Not yet consumed by any screen — `DesignSystemFragment`'s error demo uses a plain string message, not this color. Wire it in when a screen needs an error-colored visual, not just error text. |

No hardcoded hex colors belong in layout XML outside these tokens (per `CLAUDE.md`), with the standing exception of launcher icon assets.

## Font family

No custom/brand font ships today — text renders in the system default. `Base.Theme.AndroidXmlBase` sets `android:fontFamily="sans-serif"` explicitly (no visual change from the previous implicit default) so that adding a real brand font later is a one-line swap in that single theme instead of touching every text style.

## Text style tokens

Defined in `app/src/main/res/values/text_styles.xml` — 6 styles, each layered on a `TextAppearance.MaterialComponents.*` parent with `color_on_surface` and any weight override baked in:

- `TextAppearance.AndroidXmlBase.Headline` (parent `Headline6`, bold) — screen/section titles.
- `TextAppearance.AndroidXmlBase.Body` (parent `Body1`) — primary body copy.
- `TextAppearance.AndroidXmlBase.Caption` (parent `Caption`) — secondary/small text (e.g. section labels).
- `TextAppearance.AndroidXmlBase.BodyEmphasis` (parent `Body1`, `_15ssp`, bold) — bold tappable-row/button label (`dialog_prompt.xml`'s action buttons).
- `TextAppearance.AndroidXmlBase.BodyMedium` (parent `Body2`, `_14ssp`) — secondary body copy (`dialog_prompt.xml`'s message text).
- `TextAppearance.AndroidXmlBase.Micro` (parent `Caption`, `_10ssp`) — fine print (`dialog_prompt.xml`'s technical-detail text).

The last 3 exist only because `dialog_prompt.xml` already needed those exact sizes as raw, unscaled `android:textSize` — unlike the original 3 (which inherit Material's fixed, non-ssp-scaled defaults), these are explicitly set to `@dimen/_Nssp` so they participate in the sdp/ssp responsive convention below.

Apply via `android:textAppearance="@style/TextAppearance.AndroidXmlBase.<Style>"`. When an instance needs a color that differs from a tier's baked default (e.g. `BodyEmphasis` used with `color_on_primary` inside a filled button), override `android:textColor` directly on the `TextView` rather than adding a new tier — see `dialog_prompt.xml`. This is still a deliberately small scale — **don't invent a larger type scale until a real screen needs more than these 6**; a project forked from this base with its own real screens (see e.g. the FaceOTP host's `docs/DESIGN_SYSTEM.md`) will likely need to grow this further, evidenced by its own raw sizes, not speculatively ahead of time.

## Component reference

All 5 components live in `com.thanhng224.androidxmlbase.core.ui.components` (full API surface in `docs/CORE_MODULES.md`'s "`core/ui/components`" section — this section focuses on when/how to use each, not the full class listing).

### `FrameButton`

A `FrameLayout`-based button — the **only** button shape this base has built. Attrs: `app:buttonBackgroundColor`, `app:buttonCornerRadius`, `app:buttonStrokeWidth`, `app:buttonStrokeColor`, `app:buttonShape` (`rectangle` | `oval`). It is a container, not a text widget — wrap a `TextView` inside it for the label. The component marks itself as a button for accessibility and enforces a 48dp minimum touch target.

```xml
<com.thanhng224.androidxmlbase.core.ui.components.FrameButton
    android:layout_width="match_parent"
    android:layout_height="@dimen/_48sdp"
    app:buttonBackgroundColor="@color/color_primary"
    app:buttonCornerRadius="@dimen/_8sdp"
    app:buttonShape="rectangle">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="@string/design_system_primary_button"
        android:textColor="@color/color_on_primary" />

</com.thanhng224.androidxmlbase.core.ui.components.FrameButton>
```

`fragment_design_system.xml` shows two styles side by side: filled (`buttonBackgroundColor="@color/color_primary"`, no stroke) and outlined (`buttonBackgroundColor="@color/color_surface"`, `buttonStrokeColor="@color/color_primary"`, `buttonStrokeWidth="@dimen/_1sdp"`).

**`LinearButton`/`CardButton`/etc. do not exist yet.** The reference project this base ports from has 9 total button variants (all the same underlying `ButtonStyleDelegate`, composed onto a different base `View`/`ViewGroup` — a `LinearLayout`, a `CardView`, plain `TextView`, `ImageView`, ...). This base ported only `FrameButton`. Add another variant only when a real screen needs a shape `FrameButton` genuinely can't express (e.g. a button that must itself be an `ImageView`) — do not add one speculatively "for completeness." See `docs/FEATURE_TEMPLATE.md` anti-pattern 5.

### `ShadowLayout`

A `FrameLayout` that draws a soft platform shadow behind its content via elevation + a rounded outline (not a hand-drawn blur). Attrs: `app:shadowCornerRadius`, `app:shadowBackgroundColor`.

**The caller must set `android:elevation` on the instance itself** — `ShadowLayout` does not force its own elevation internally, it only supplies the rounded outline the elevation shadow renders against:

```xml
<com.thanhng224.androidxmlbase.core.ui.components.ShadowLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:elevation="@dimen/_4sdp"
    app:shadowBackgroundColor="@color/color_surface"
    app:shadowCornerRadius="@dimen/_8sdp">
    <!-- content -->
</com.thanhng224.androidxmlbase.core.ui.components.ShadowLayout>
```

### `ThemedSwitch`

A thin `MaterialSwitch` subclass (`com.google.android.material.materialswitch.MaterialSwitch`) that retints the track/thumb from this base's color tokens instead of the platform default. No custom attrs — use it exactly like a `Switch`/`MaterialSwitch` (`android:text`, etc.). It does not reimplement touch/accessibility handling; that stays inherited from `MaterialSwitch`.

### `StyledSnackbar`

`StyledSnackbar.show(anchorView: View, message: String, duration: Int = Snackbar.LENGTH_SHORT)`. Backed by `Snackbar`, not a `Toast` view — **takes a `View` anchor, not a `Context`**, because `Toast.setView` is deprecated since API 30 and custom-view toasts are suppressed while the host app is backgrounded, whereas a `Snackbar` anchored to a visible `View` always renders reliably in the foreground. Typical call site: `StyledSnackbar.show(binding.root, getString(R.string.some_message))`.

### Single-choice controls

Use a Material single-choice dialog for a short, mutually exclusive settings value such as app language or appearance. `SettingsActivity` is the live example: the settings screen remains a scannable list, while the dialog owns the finite choice interaction.

### Bottom navigation

`activity_appshell_main.xml` uses `Widget.Material3Expressive.BottomNavigationView` inside a rounded `MaterialCardView`. Keep all three labels visible, use the Material active indicator instead of a custom animation, and map item IDs directly to top-level destination IDs in `main_navigation.xml`. The bottom bar is limited to 3-5 equally important destinations; secondary actions such as Settings belong in the app bar.

### Icon color

Monochrome vector sources use `color_on_surface`, never a hardcoded white fill. Apply a contextual tint only where the icon sits on a different semantic container (for example, `color_on_primary_container` in a settings-row icon). `Widget.AndroidXmlBase.Toolbar` explicitly supplies `colorControlNormal` and navigation-icon tint from `color_on_surface`, so app-bar action icons remain legible in both light and dark themes. White remains valid only for a foreground deliberately drawn on a colored container, such as the success checkmark.

## sdp/ssp convention

This base uses `com.intuit.sdp:sdp-android` / `com.intuit.ssp:ssp-android` (both pure resource-only artifacts) for responsive dimensions, applied as `@dimen/_<n>sdp` (density-independent, scales with `smallestScreenWidthDp`) and `@dimen/_<n>ssp` (same, for text sizes) directly in layout XML:

```xml
android:layout_marginTop="@dimen/_16sdp"
app:buttonCornerRadius="@dimen/_8sdp"
```

**No hardcoded non-zero `dp`/`sp` literal belongs in a layout XML this convention covers** — use the sdp/ssp resources consistently in `activity_appshell_main.xml`, `fragment_appshell_home.xml`, and `fragment_demo.xml`.

**There is no `Int.sdp()` / `Int.ssp()` Kotlin extension function, and that is a deliberate decision, not a gap.** The Phase 5 plan resolved this explicitly:

> "sdp/ssp resolved as: use the established `com.intuit.sdp`/`com.intuit.ssp` XML-dimens libraries directly (`@dimen/_16sdp` in layouts), not hand-rolled `Int.sdp()`/`Int.ssp()` Kotlin extension functions... The library's whole purpose is XML dimension resources auto-generated per `smallestScreenWidthDp` bucket — a Kotlin extension duplicating that is unnecessary; nothing in this codebase does programmatic (non-XML) dimension math."

If a future screen needs a *programmatic* (non-XML) dp/sp conversion, that would be a new, deliberate addition — not something to assume already exists.

Related: `core.ui.responsive.ResponsiveContextWrapper` clamps `smallestScreenWidthDp` into `[320, 480]` before sdp/ssp resources resolve, so the sdp/ssp buckets stay bounded even on tablets/foldables — see `docs/CORE_MODULES.md`'s "`core/ui/responsive`" section.

## Live reference

`sample/designsystem`'s `DesignSystemFragment` / `app/src/main/res/layout/fragment_design_system.xml` inflates every component and token described above in one screen: all 6 text styles (headline/body/caption/body-emphasis/body-medium/micro), a filled `FrameButton`, an outlined `FrameButton`, a `ShadowLayout` card, a `ThemedSwitch`, a `FrameButton` that triggers `StyledSnackbar`, and a 3-button `ResultState` (loading/success/error) demo driven by `DesignSystemViewModel`. When adding a new component or token, add it to this screen too so it stays the working reference.
