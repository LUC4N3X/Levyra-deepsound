package com.luc4n3x.levyra.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

private const val CONTENT_TIMEOUT_MS = 5_000L
private const val STEP_TIMEOUT_MS = 1_500L
private const val GONE_TIMEOUT_MS = 3_000L
private const val MAX_ONBOARDING_STEPS = 4

fun resolveTargetPackageName(): String =
    InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: error("targetAppId was not passed as an instrumentation runner argument")

fun MacrobenchmarkScope.completeOnboardingIfPresent() {
    repeat(MAX_ONBOARDING_STEPS) {
        val skip = device.wait(Until.findObject(By.text("Skip and continue")), STEP_TIMEOUT_MS)
        if (skip != null) {
            skip.click()
            device.wait(Until.gone(By.text("Skip and continue")), GONE_TIMEOUT_MS)
            return
        }
        val next = device.findObject(By.text("Next")) ?: return
        next.click()
        device.waitForIdle()
    }
}

fun MacrobenchmarkScope.waitForContent() {
    device.waitForIdle()
    device.wait(Until.hasObject(By.scrollable(true)), CONTENT_TIMEOUT_MS)
}

fun MacrobenchmarkScope.loadHome() {
    completeOnboardingIfPresent()
    waitForContent()
}

fun MacrobenchmarkScope.scrollCurrentScreen() {
    val scrollable = device.findObject(By.scrollable(true)) ?: return
    scrollable.setGestureMargin(device.displayWidth / 5)
    repeat(2) {
        scrollable.fling(Direction.DOWN)
        device.waitForIdle()
    }
    scrollable.fling(Direction.UP)
    device.waitForIdle()
}

fun MacrobenchmarkScope.openTab(contentDescription: String) {
    val tab = device.wait(Until.findObject(By.desc(contentDescription)), CONTENT_TIMEOUT_MS) ?: return
    tab.click()
    device.waitForIdle()
}

fun MacrobenchmarkScope.startPlaybackIfPossible() {
    val play = device.wait(Until.findObject(By.desc("Play")), STEP_TIMEOUT_MS) ?: return
    play.click()
    device.waitForIdle()
}

fun MacrobenchmarkScope.runLevyraJourney() {
    loadHome()
    scrollCurrentScreen()

    openTab("Explore")
    waitForContent()
    scrollCurrentScreen()

    openTab("Library")
    waitForContent()
    scrollCurrentScreen()

    openTab("Home")
    waitForContent()
    startPlaybackIfPossible()

    openTab("Player")
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()
}
