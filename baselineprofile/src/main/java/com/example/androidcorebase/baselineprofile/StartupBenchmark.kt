package com.example.androidcorebase.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val APP_PACKAGE_NAME = "com.example.androidcorebase"
private const val MEASURED_ITERATIONS = 10

/**
 * Compares cold-start TTID under [CompilationMode.None] and [CompilationMode.Partial] (requiring
 * the committed `baseline-prof.txt`), which is exactly what the baseline profile is supposed to
 * improve.
 *
 * The measured block is only `pressHome()` + `startActivityAndWait()`. [StartupTimingMetric] times
 * that launch and nothing else, so any further navigation inside the block would not be measured -
 * it would only inflate wall-clock time and dilute [FrameTimingMetric] with frames from unrelated
 * screens. Under [CompilationMode.None] the app also runs fully interpreted, where a multi-screen
 * journey (first Compose composition included) is slow enough to time the run out entirely.
 *
 * [CriticalJourney] still drives profile *collection* in [BaselineProfileGenerator], so the
 * committed profile covers the same code paths a real session touches. Meaningful frame-timing
 * numbers would need a separate scroll/interaction benchmark; this class does not pretend to
 * provide them.
 *
 * See `docs/performance/BASELINE_PROFILE_RESULTS.md` for recorded runs.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCompilationNone() = startup(CompilationMode.None())

    @Test
    fun startupCompilationBaselineProfile() = startup(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

    private fun startup(compilationMode: CompilationMode) =
        benchmarkRule.measureRepeated(
            packageName = APP_PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
            iterations = MEASURED_ITERATIONS,
            startupMode = StartupMode.COLD,
            compilationMode = compilationMode,
        ) {
            pressHome()
            startActivityAndWait()
        }
}
