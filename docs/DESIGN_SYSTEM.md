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

No custom/brand font ships today — text renders in the system default. `Base.Theme.AndroidCoreBase` sets `android:fontFamily="sans-serif"` explicitly (no visual change from the previous implicit default) so that adding a real brand font later is a one-line swap in that single theme instead of touching every text style.

## Text style tokens

Defined in `app/src/main/res/values/text_styles.xml` — 6 styles, each layered on a `TextAppearance.MaterialComponents.*` parent with `color_on_surface` and any weight override baked in:

- `TextAppearance.AndroidCoreBase.Headline` (parent `Headline6`, bold) — screen/section titles.
- `TextAppearance.AndroidCoreBase.Body` (parent `Body1`) — primary body copy.
- `TextAppearance.AndroidCoreBase.Caption` (parent `Caption`) — secondary/small text (e.g. section labels).
- `TextAppearance.AndroidCoreBase.BodyEmphasis` (parent `Body1`, 15sp, bold) — bold tappable-row/button label (`dialog_prompt.xml`'s action buttons).
- `TextAppearance.AndroidCoreBase.BodyMedium` (parent `Body2`, 14sp) — secondary body copy (`dialog_prompt.xml`'s message text).
- `TextAppearance.AndroidCoreBase.Micro` (parent `Caption`, 10sp) — fine print (`dialog_prompt.xml`'s technical-detail text).

The last 3 exist because `dialog_prompt.xml` needs those exact sizes as raw `android:textSize` — unlike the original 3 (which inherit Material's fixed defaults), these set `android:textSize` explicitly to `@dimen/core_text_size_<n>` (see the "Spacing, radius, size & text-size scale" section below). That token is a fixed `sp` value now — until Phase 2 (2026-07-29) it was `@dimen/_Nssp` from `com.intuit.ssp`; see `docs/MODERNIZATION.md` D1 for why that dependency was retired.

Apply via `android:textAppearance="@style/TextAppearance.AndroidCoreBase.<Style>"`. When an instance needs a color that differs from a tier's baked default (e.g. `BodyEmphasis` used with `color_on_primary` inside a filled button), override `android:textColor` directly on the `TextView` rather than adding a new tier — see `dialog_prompt.xml`. This is still a deliberately small scale — **don't invent a larger type scale until a real screen needs more than these 6**; a project forked from this base with its own real screens (see e.g. the FaceOTP host's `docs/DESIGN_SYSTEM.md`) will likely need to grow this further, evidenced by its own raw sizes, not speculatively ahead of time.

## Component reference

All 5 components live in `com.thanhng224.androidcorebase.core.ui.components` (full API surface in `docs/CORE_MODULES.md`'s "`core/ui/components`" section — this section focuses on when/how to use each, not the full class listing).

### `FrameButton`

A `FrameLayout`-based button — the **only** button shape this base has built. Attrs: `app:buttonBackgroundColor`, `app:buttonCornerRadius`, `app:buttonStrokeWidth`, `app:buttonStrokeColor`, `app:buttonShape` (`rectangle` | `oval`). It is a container, not a text widget — wrap a `TextView` inside it for the label. The component marks itself as a button for accessibility and enforces a 48dp minimum touch target.

```xml
<com.thanhng224.androidcorebase.core.ui.components.FrameButton
    android:layout_width="match_parent"
    android:layout_height="@dimen/core_size_48"
    app:buttonBackgroundColor="@color/color_primary"
    app:buttonCornerRadius="@dimen/core_radius_8"
    app:buttonShape="rectangle">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="@string/design_system_primary_button"
        android:textColor="@color/color_on_primary" />

</com.thanhng224.androidcorebase.core.ui.components.FrameButton>
```

`fragment_design_system.xml` shows two styles side by side: filled (`buttonBackgroundColor="@color/color_primary"`, no stroke) and outlined (`buttonBackgroundColor="@color/color_surface"`, `buttonStrokeColor="@color/color_primary"`, `buttonStrokeWidth="@dimen/core_stroke_width"`).

**`LinearButton`/`CardButton`/etc. do not exist yet.** The reference project this base ports from has 9 total button variants (all the same underlying `ButtonStyleDelegate`, composed onto a different base `View`/`ViewGroup` — a `LinearLayout`, a `CardView`, plain `TextView`, `ImageView`, ...). This base ported only `FrameButton`. Add another variant only when a real screen needs a shape `FrameButton` genuinely can't express (e.g. a button that must itself be an `ImageView`) — do not add one speculatively "for completeness." See `docs/FEATURE_TEMPLATE.md` anti-pattern 5.

### `ShadowLayout`

A `FrameLayout` that draws a soft platform shadow behind its content via elevation + a rounded outline (not a hand-drawn blur). Attrs: `app:shadowCornerRadius`, `app:shadowBackgroundColor`.

**The caller must set `android:elevation` on the instance itself** — `ShadowLayout` does not force its own elevation internally, it only supplies the rounded outline the elevation shadow renders against:

```xml
<com.thanhng224.androidcorebase.core.ui.components.ShadowLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:elevation="@dimen/core_space_4"
    app:shadowBackgroundColor="@color/color_surface"
    app:shadowCornerRadius="@dimen/core_radius_8">
    <!-- content -->
</com.thanhng224.androidcorebase.core.ui.components.ShadowLayout>
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

Monochrome vector sources use `color_on_surface`, never a hardcoded white fill. Apply a contextual tint only where the icon sits on a different semantic container (for example, `color_on_primary_container` in a settings-row icon). `Widget.AndroidCoreBase.Toolbar` explicitly supplies `colorControlNormal` and navigation-icon tint from `color_on_surface`, so app-bar action icons remain legible in both light and dark themes. White remains valid only for a foreground deliberately drawn on a colored container, such as the success checkmark.

## Spacing, radius, size & text-size scale

**As of Phase 2 (2026-07-29) this base no longer uses `com.intuit.sdp`/`com.intuit.ssp`.** Dimensions
are named, fixed `dp`/`sp` constants in `core/src/main/res/values/dimens.xml`, resolved the same on
every device — no `smallestScreenWidthDp`-based scaling. See `docs/MODERNIZATION.md` finding F5 and
decision D1 for why: the sdp/ssp buckets and the (also-removed) `ResponsiveContextWrapper` clamp
fought each other, and clamping `smallestScreenWidthDp` broke `values-sw600dp/` qualifier resolution
for tablets/large screens — the opposite of what a responsive strategy should do.

Four token families, each named after what it represents rather than a raw number, so a layout
reads as intent instead of arithmetic:

```xml
android:layout_marginTop="@dimen/core_space_16"
app:coreButtonCornerRadius="@dimen/core_radius_8"
android:layout_width="@dimen/core_size_40"
app:strokeWidth="@dimen/core_stroke_width"
android:textSize="@dimen/core_text_size_14"
```

- **`core_space_<n>`** — margins, padding, gaps, elevation. (`2, 4, 6, 8, 10, 12, 16, 20, 24, 32, 72`)
- **`core_radius_<n>`** — card/button/drawable corner radius. (`8, 12, 16, 20, 24, 28`)
- **`core_size_<n>`** — a view's own fixed width/height/minWidth/minHeight (icon boxes, avatars,
  touch targets, illustrations) — kept distinct from `core_space_<n>` even where the number
  coincides (e.g. `core_size_24` vs `core_space_24`), because a view's own dimension and a gap
  between views are different design decisions that happen to share a value today.
- **`core_stroke_width`** — the one hairline thickness this base uses, for card/button strokes and
  divider lines alike.
- **`core_text_size_<n>`** — fixed `sp` sizes, replacing `@dimen/_Nssp` (used by the 3 raw-size text
  styles above and `Widget.AndroidCoreBase.IconButton`).

**No hardcoded non-zero `dp`/`sp` literal belongs in a layout XML** — use these tokens consistently,
same as the sdp/ssp convention required before it.

**There is no `Int.dp()` / `Int.sp()` Kotlin extension function.** Nothing in this codebase does
programmatic (non-XML) dimension math; if a future screen needs one, that is a new, deliberate
addition — not something to assume already exists.

**Migration note:** at the base's ~360dp calibration width, `@dimen/_16sdp` already resolved to
~16dp, so swapping to `@dimen/core_space_16` is a visual no-op on ordinary phones. What *did*
change on purpose is tablets/large screens: sdp/ssp scaled every dimension up continuously with
screen width; fixed tokens do not. A screen that genuinely needs a different value on a large
screen should add one to `values-sw600dp/dimens.xml`, not rely on scaling.

## Live reference

`sample/designsystem`'s `DesignSystemFragment` / `app/src/main/res/layout/fragment_design_system.xml` inflates every component and token described above in one screen: all 6 text styles (headline/body/caption/body-emphasis/body-medium/micro), a filled `FrameButton`, an outlined `FrameButton`, a `ShadowLayout` card, a `ThemedSwitch`, a `FrameButton` that triggers `StyledSnackbar`, and a 3-button `ResultState` (loading/success/error) demo driven by `DesignSystemViewModel`. When adding a new component or token, add it to this screen too so it stays the working reference.
