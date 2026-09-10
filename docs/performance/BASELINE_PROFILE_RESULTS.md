# Baseline Profile Results

## Status

| Gate | State |
|---|---|
| Profile generation (`:app:generateBaselineProfile`) | **Done** |
| Macrobenchmark measurement (`:baselineprofile:connectedCheck`) | **Done** |

## Target

| | |
|---|---|
| Device | Samsung Galaxy Z Fold 4, `SM-F936N` |
| Android | 16 (API 36) |
| Tree | `13d01aa` plus the `StartupBenchmark` change committed alongside this file |
| Battery | 37%+ and charging (`androidx.benchmark` refuses to measure below 20%) |

## Generated profile

| | |
|---|---|
| Command | `./gradlew :app:generateBaselineProfile` |
| Output | `app/src/release/generated/baselineProfiles/baseline-prof.txt` (committed) |
| Rules | 56,130 total — 354 from `com.thanhng224` (`:core`), 706 from `com.example.androidcorebase` (`:app`) |
| Journey | `CriticalJourney` — Home → Demo (increment) → UI Kit (scroll to Compose interop) → Settings (theme change) → Home |

Entirely plugin-produced. No rule written or edited by hand.

## Measured startup

`./gradlew :baselineprofile:connectedCheck`, 10 iterations per mode, `StartupMode.COLD`.

| Metric | `CompilationMode.None()` | `CompilationMode.Partial(Require)` | Δ |
|---|---|---|---|
| TTID median | **262.5 ms** | **245.7 ms** | **−16.8 ms (−6.4%)** |
| TTID min | 250.2 ms | 239.6 ms | −10.6 ms |
| TTID max | 298.2 ms | 266.9 ms | −31.3 ms |
| Iterations | 10 | 10 | |

Results JSON and 20 Perfetto traces (10 per mode):

```
baselineprofile/build/outputs/connected_android_test_additional_output/benchmarkRelease/connected/SM-F936N - 16/
  com.example.androidcorebase.baselineprofile-benchmarkData.json
  StartupBenchmark_startupCompilation{None,BaselineProfile}_iterNNN_*.perfetto-trace
```

The profile is a consistent win across median, min and max — the whole distribution shifts, not
just the midpoint. The magnitude is modest because the target is a current flagship running a small
starter app; baseline profiles pay off most on low-end hardware and larger codebases. Do not quote
−6.4% as a general figure.

## What this does not measure

`FrameTimingMetric` is declared but reports only `frameCount` (median 4 in both modes) and no
duration percentiles. That is expected: the measured block is a cold launch, which produces a
handful of frames and no sustained rendering. **There is no frame-timing evidence here.**

Meaningful frame numbers need a separate benchmark that scrolls or animates for a sustained period.
That is deliberately not part of this gate.

### Why the measured block is only the launch

`StartupBenchmark`'s `measureRepeated` block is `pressHome()` + `startActivityAndWait()` and nothing
else. An earlier revision replayed the whole `CriticalJourney` inside it, which was wrong twice
over: `StartupTimingMetric` times the launch only, so later navigation was never measured and just
inflated wall-clock; and under `CompilationMode.None()` the app runs fully interpreted, where a
five-screen journey (first Compose composition included) does not finish inside any reasonable
timeout — the run failed with `UI Kit tab never rendered` even at a 20 s ceiling.

`CriticalJourney` still drives profile *collection*, so the committed profile covers the code paths
a real session touches.

## Device note: Xiaomi/HyperOS cannot run this gate

The same commands fail on Xiaomi `2404ARN45A` (also API 36), and not for any project reason:

```
I ActivityManager: Enqueued broadcast Intent { ... INSTALL_PROFILE ... }: 0
D ProcessStarter: proc frequent died! proc = com.example.androidcorebase callerPkg = null
```

HyperOS refuses to cold-start the process to deliver profileinstaller's handshake broadcast, because
the benchmark's own repeated `force-stop` calls mark the app as "frequently died". Sending the
broadcast by hand shows the split exactly:

| App process state | `am broadcast` result |
|---|---|
| Alive | `result=1` — receiver runs |
| Cold (after `force-stop`) | `result=0` — receiver never runs |

Ruled out on that device: `profileinstaller` is 1.4.1 (above the 1.3.0 the error text suggests); the
merged `benchmarkRelease` manifest declares `ProfileInstallReceiver` correctly (`enabled`,
`exported`, `permission="android.permission.DUMP"`) plus the `ProfileInstallerInitializer`
meta-data; `dumpsys package` resolves all four profileinstaller actions; and `shell` holds `DUMP`.
Tried and insufficient: `cmd appops set <pkg> RUN_IN_BACKGROUND allow`,
`dumpsys deviceidle whitelist +<pkg>`, `am broadcast --include-stopped-packages`. MIUI exposes no
autostart appop over adb on that build.

Macrobenchmark must `force-stop` to measure cold startup, so it always hits the blocked path. Use a
Samsung/Pixel/AOSP device or the Google-API emulator. To unblock a Xiaomi anyway: Security app →
Autostart → enable for the app, and/or Developer options → turn off "MIUI optimization" (reboot
required). Neither is settable over adb.

## Reproducing

```bash
./gradlew :app:generateBaselineProfile     # regenerate and copy the profile into app/src/release
./gradlew :baselineprofile:connectedCheck  # measure None vs Partial(Require)
```

Record device model, API level, tree SHA and battery state alongside any numbers added here.
