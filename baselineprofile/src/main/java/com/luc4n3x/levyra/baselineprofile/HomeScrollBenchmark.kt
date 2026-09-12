package com.luc4n3x.levyra.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScrollBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun homeScrollWithBaselineProfile() {
        rule.measureRepeated(
            packageName = "com.luc4n3x.levyra",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Require
            ),
            iterations = 6,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                device.waitForIdle()
                dismissOnboardingIfPresent()
                device.waitForIdle()
            }
        ) {
            repeat(5) {
                device.swipe(
                    device.displayWidth / 2,
                    device.displayHeight * 4 / 5,
                    device.displayWidth / 2,
                    device.displayHeight / 4,
                    10
                )
            }
            repeat(5) {
                device.swipe(
                    device.displayWidth / 2,
                    device.displayHeight / 4,
                    device.displayWidth / 2,
                    device.displayHeight * 4 / 5,
                    10
                )
            }
        }
    }
}
