package com.luc4n3x.levyra.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

internal fun MacrobenchmarkScope.launchAndExerciseLevyra() {
    pressHome()
    startActivityAndWait()
    device.waitForIdle()
    dismissOnboardingIfPresent()
    device.waitForIdle()
    repeat(3) {
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight * 4 / 5,
            device.displayWidth / 2,
            device.displayHeight / 3,
            12
        )
        device.waitForIdle()
    }
    device.findObject(By.descContains("Libr"))?.click()
        ?: device.findObject(By.textContains("Libr"))?.click()
        ?: device.findObject(By.descContains("Library"))?.click()
        ?: device.findObject(By.textContains("Library"))?.click()
    device.waitForIdle()
    device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 0.6f)
    device.waitForIdle()
}

private fun MacrobenchmarkScope.dismissOnboardingIfPresent() {
    val labels = listOf("Continua", "Avanti", "Inizia", "Fatto", "Continue", "Next", "Start", "Done", "Salta", "Skip")
    repeat(6) {
        val button = labels.asSequence()
            .mapNotNull { label -> device.findObject(By.textContains(label)) }
            .firstOrNull()
            ?: return
        button.click()
        device.wait(Until.gone(By.text(button.text.orEmpty())), 1_000L)
        device.waitForIdle()
    }
}
