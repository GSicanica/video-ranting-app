package com.youtube.rating.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmarks cold startup speed with/without Baseline Profiles.
 *
 * Run via Gradle:
 * `./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest`
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupCompilationNone() =
        benchmark(compilationMode = CompilationMode.None())

    @Test
    fun startupCompilationBaselineProfiles() =
        benchmark(compilationMode = CompilationMode.Partial(BaselineProfileMode.Require))

    private fun benchmark(compilationMode: CompilationMode) {
        // Startup macrobenchmarks are useful but can be flaky on some OEM firmwares due to Perfetto/UiAutomation.
        // Keep them opt-in so baseline profile generation stays reliable.
        val enabled = InstrumentationRegistry.getArguments()
            .getString("runStartupBenchmarks")
            ?.equals("true", ignoreCase = true) == true
        Assume.assumeTrue("Startup benchmarks disabled (pass -PrunStartupBenchmarks=true to enable).", enabled)

        try {
            rule.measureRepeated(
                packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
                    ?: throw IllegalStateException("targetAppId not passed as instrumentation runner arg"),
                metrics = listOf(StartupTimingMetric()),
                compilationMode = compilationMode,
                startupMode = StartupMode.COLD,
                iterations = 10,
                setupBlock = { pressHome() },
                measureBlock = {
                    startActivityAndWait()
                    // TODO: Wait for fully-drawn if you call reportFullyDrawn in the app.
                }
            )
        } catch (e: IllegalStateException) {
            // Some OEM builds (notably certain Samsung firmwares) can fail to stop the perfetto daemon,
            // which causes macrobenchmarks to fail even though the app is fine. Skip in that case.
            if (e.message?.contains("Failed to stop", ignoreCase = true) == true &&
                e.message?.contains("perfetto", ignoreCase = true) == true
            ) {
                Assume.assumeNoException("Perfetto stop failed on this device/firmware", e)
            }
            throw e
        }
    }
}
