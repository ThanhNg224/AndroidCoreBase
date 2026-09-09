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
 * Measures cold-start TTID and frame timing across [CriticalJourney] under [CompilationMode.None]
 * and [CompilationMode.Partial] (requiring the committed `baseline-prof.txt`), so the two modes are
 * directly comparable. See `docs/performance/BASELINE_PROFILE_RESULTS.md` for the recorded evidence
 * from an authorized device run.
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
            CriticalJourney.execute(device, packageName)
        }
}
