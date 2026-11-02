package com.luc4n3x.levyra.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

private val OnboardingStep = Pattern.compile("^[1-9]/[1-9]$")
private val OnboardingActions = listOf("Get started", "Inizia", "Allow", "Consenti", "Skip", "Salta", "Continue", "Continua", "Next", "Successivo", "Avanti", "Done", "Fatto")
private const val ONBOARDING_ACTION_LIMIT = 12
private const val SETTLE_TIMEOUT_MS = 2_000L

internal fun MacrobenchmarkScope.launchAndExerciseLevyra() {
    pressHome()
    startActivityAndWait()
    device.waitForIdle()
    completeOnboardingIfPresent()
    scrollCurrentScreen(times = 3)
    listOf("Search", "Explore", "Library", "Home").forEach { tab ->
        openTab(tab)
        scrollCurrentScreen(times = 2)
    }
}

private fun MacrobenchmarkScope.completeOnboardingIfPresent() {
    repeat(ONBOARDING_ACTION_LIMIT) {
        val onboarding = device.hasObject(By.text(OnboardingStep)) ||
            device.hasObject(By.text("Get started")) ||
            device.hasObject(By.text("Inizia")) ||
            device.hasObject(By.text("Allow"))
        if (!onboarding) return
        val action = OnboardingActions.asSequence()
            .mapNotNull { label -> device.findObject(By.textStartsWith(label)) }
            .firstOrNull()
            ?: return
        val label = action.text.orEmpty()
        action.click()
        device.wait(Until.gone(By.text(label)), SETTLE_TIMEOUT_MS)
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.openTab(label: String) {
    val labels = when (label) {
        "Search" -> listOf("Search", "Cerca")
        "Explore" -> listOf("Explore", "Esplora")
        "Library" -> listOf("Library", "Libreria")
        else -> listOf(label)
    }
    val namedTab = labels.firstNotNullOfOrNull { text -> device.findObject(By.text(text)) ?: device.findObject(By.desc(text)) }
    val slot = listOf("Home", "Search", "Explore", "Library").indexOf(label)
    val tabs = if (namedTab == null) device.findObjects(By.focusable(true)).filter { candidate ->
        val bounds = candidate.visibleBounds
        bounds.bottom >= device.displayHeight * 9 / 10 && bounds.width() in device.displayWidth / 8..device.displayWidth / 3
    }.sortedBy { it.visibleBounds.left } else emptyList()
    val tab = namedTab ?: tabs.takeIf { it.size == 4 }?.getOrNull(slot)
    checkNotNull(tab) { "Navigation tab unavailable: $label" }
    tab.click()
    device.waitForIdle()
}

private fun MacrobenchmarkScope.scrollCurrentScreen(times: Int) {
    repeat(times) {
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight * 4 / 5,
            device.displayWidth / 2,
            device.displayHeight / 3,
            12
        )
        device.waitForIdle()
    }
}
