package com.luc4n3x.levyra.baselineprofile

import android.util.Log
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

private val OnboardingStep = Pattern.compile("^[1-9]/[1-9]$")
private val OnboardingActions = listOf("Allow", "Consenti", "Get started", "Inizia", "Skip", "Salta", "Continue", "Continua", "Next", "Successivo", "Avanti", "Done", "Fatto")
private const val ONBOARDING_ACTION_LIMIT = 12
private const val NAVIGATION_REVEAL_ATTEMPTS = 3
private const val ProfileLogTag = "LevyraProfile"
private const val SETTLE_TIMEOUT_MS = 2_000L
private const val SEARCH_TIMEOUT_MS = 10_000L
private const val ProfileArtistQuery = "Taylor Swift"
private const val LatestReleaseLabel = "Latest release"

internal fun MacrobenchmarkScope.launchLevyra() {
    pressHome()
    startActivityAndWait()
    device.waitForIdle()
    completeOnboardingIfPresent()
}

internal fun MacrobenchmarkScope.launchAndExerciseLevyra() {
    launchLevyra()
    scrollCurrentScreen(times = 3)
    val tabs = listOf("Search", "Explore", "Library", "Home")
    val failedTabs = tabs.filter { tab ->
        runCatching {
            openTab(tab)
            scrollCurrentScreen(times = 2)
        }.isFailure
    }
    check(failedTabs.size < tabs.size) { "No navigation tab could be exercised: $failedTabs" }
    if (failedTabs.isNotEmpty()) Log.w(ProfileLogTag, "Profile collection skipped navigation tabs: $failedTabs")
    runCatching { exerciseArtistAndAlbum() }
}

private fun MacrobenchmarkScope.exerciseArtistAndAlbum() {
    openTab("Search")
    val field = device.wait(Until.findObject(By.clazz("android.widget.EditText")), SEARCH_TIMEOUT_MS) ?: return
    field.click()
    field.text = ProfileArtistQuery
    device.pressEnter()
    val artist = device.wait(Until.findObject(By.text(ProfileArtistQuery).clazz("android.widget.TextView")), SEARCH_TIMEOUT_MS) ?: return
    artist.click()
    device.waitForIdle()
    device.wait(Until.hasObject(By.text(LatestReleaseLabel)), SEARCH_TIMEOUT_MS)
    scrollCurrentScreen(times = 1)
    val release = device.findObject(By.text(LatestReleaseLabel))
    if (release != null) {
        release.click()
        device.waitForIdle()
        device.wait(Until.hasObject(By.textContains(ProfileArtistQuery)), SEARCH_TIMEOUT_MS)
        scrollCurrentScreen(times = 3)
        device.pressBack()
        device.waitForIdle()
    }
    scrollCurrentScreen(times = 4)
    device.pressBack()
    device.waitForIdle()
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
    val findNamed = { labels.firstNotNullOfOrNull { text -> device.findObject(By.text(text)) ?: device.findObject(By.desc(text)) } }
    var namedTab = findNamed()
    repeat(NAVIGATION_REVEAL_ATTEMPTS) {
        if (namedTab != null) return@repeat
        device.swipe(device.displayWidth / 2, device.displayHeight / 3, device.displayWidth / 2, device.displayHeight * 4 / 5, 12)
        device.waitForIdle()
        device.wait(Until.hasObject(By.text(labels.first())), SETTLE_TIMEOUT_MS)
        namedTab = findNamed()
    }
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
