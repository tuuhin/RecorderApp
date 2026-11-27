package com.sam.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupWithBaselineProfile() = startup(CompilationMode.Partial())

    @Test
    fun startupWithNoCompilation() = startup(CompilationMode.None())

    private fun startup(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        iterations = 5,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), UI_TIMEOUT_MS)
    }

    companion object {
        // The macrobenchmark runs against the app's `benchmark` variant (non-debuggable,
        // profileable), whose applicationId carries the `.benchmark` suffix.
        private const val PACKAGE_NAME = "com.eva.recorderapp.benchmark"
        private const val UI_TIMEOUT_MS = 5_000L
    }
}
