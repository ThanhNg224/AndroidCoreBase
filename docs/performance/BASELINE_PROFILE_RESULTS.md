# Baseline Profile Results

## Status

| Gate | State |
|---|---|
| Profile generation (`:app:generateBaselineProfile`) | **Done** — see below |
| Macrobenchmark measurement (`:baselineprofile:connectedCheck`) | **Pending** — no unrestricted target available yet |

## Generated profile

| | |
|---|---|
| Build SHA | `e37b0c8` |
| Target | `2404ARN45A`, Android 16 (API 36) |
| Command | `./gradlew :app:generateBaselineProfile` |
| Output | `app/src/release/generated/baselineProfiles/baseline-prof.txt` (committed) |
| Rules | 56,130 total — 354 from `com.thanhng224` (`:core`), 706 from `com.example.androidcorebase` (`:app`) |
| Journey | `CriticalJourney` — Home → Demo (increment) → UI Kit (scroll to Compose interop) → Settings (theme change) → Home |

The profile is entirely plugin-produced. No rule was written or edited by hand.

## Measurement: pending, and why

`:baselineprofile:connectedBenchmarkReleaseAndroidTest` fails on the target above:

```
startupCompilationBaselineProfile: The baseline profile install broadcast was not received.
startupCompilationNone:            The DROP_SHADER_CACHE broadcast was not received.
```

This is an OEM restriction on the target device, not a project defect. Evidence:

1. `androidx.profileinstaller` is **1.4.1**, above the 1.3.0 minimum the error text suggests.
2. The merged `benchmarkRelease` manifest declares the receiver correctly:
   `androidx.profileinstaller.ProfileInstallReceiver`, `enabled="true"`, `exported="true"`,
   `permission="android.permission.DUMP"`, plus the `ProfileInstallerInitializer` meta-data on
   `androidx.startup.InitializationProvider`.
3. `dumpsys package` resolves all four profileinstaller actions to that receiver on-device.
4. The `shell` user holds `android.permission.DUMP` (`granted=true`).
5. Sending the broadcast by hand shows the split precisely:

   | App process state | `am broadcast` result |
   |---|---|
   | Alive (app launched) | `result=1` — receiver runs |
   | Cold (after `force-stop`) | `result=0` — receiver never runs |

6. `logcat` during the cold attempt shows HyperOS/MIUI refusing the process start:

   ```
   I ActivityManager: Enqueued broadcast Intent { ... INSTALL_PROFILE ... }: 0
   D ProcessStarter: proc frequent died! proc = com.example.androidcorebase callerPkg = null
   ```

Macrobenchmark must `force-stop` the app to measure cold startup, so it always hits the blocked
path. Tried and insufficient: `cmd appops set <pkg> RUN_IN_BACKGROUND allow`,
`dumpsys deviceidle whitelist +<pkg>`, `am broadcast --include-stopped-packages`. MIUI exposes no
autostart appop over adb on this build.

## How to complete this gate

Either:

- **Preferred — benchmark on an unrestricted target.** A Pixel/AOSP device or the Google-API
  emulator used by the `baseline-profile` CI job. AndroidX recommends a physical unrestricted device
  for timing anyway.
- **Or unblock this device manually.** MIUI Security app → Autostart → enable for the app, and/or
  Developer options → turn off "MIUI optimization" (needs a reboot). Neither is settable over adb.

Then run and fill in the table below:

```bash
./gradlew :baselineprofile:connectedCheck
```

| Metric | `CompilationMode.None()` | `CompilationMode.Partial(Require)` |
|---|---|---|
| Median TTID | _pending_ | _pending_ |
| Frame P50 / P90 / P99 | _pending_ | _pending_ |
| Iterations | _pending_ | _pending_ |
| Report / trace path | _pending_ | _pending_ |

Record target model, API level, and build SHA alongside the numbers. Do not fill this in from a
run on a restricted device — the blocked broadcast makes `Partial(Require)` unrepresentative.
