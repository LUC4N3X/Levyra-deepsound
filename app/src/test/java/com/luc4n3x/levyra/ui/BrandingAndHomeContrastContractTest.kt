package com.luc4n3x.levyra.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandingAndHomeContrastContractTest {
    private val appSource: String by lazy {
        Files.readString(
            resolveRepoPath(
                "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
                "src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"
            )
        )
    }

    @Test
    fun `in app Levyra logo stays synced with the launcher emblem`() {
        val inAppLogo = Files.readAllBytes(
            resolveRepoPath(
                "app/src/main/res/drawable/levyra_logo.png",
                "src/main/res/drawable/levyra_logo.png"
            )
        )
        val launcherEmblem = Files.readAllBytes(
            resolveRepoPath(
                "app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png",
                "src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png"
            )
        )

        assertArrayEquals(launcherEmblem, inAppLogo)
    }

    @Test
    fun `home header keeps greeting readable over bright artwork in dark theme`() {
        val dayPart = functionBlock("private fun HomeGreetingDayPart(")
        val name = functionBlock("private fun HomeGreetingName(")
        val headerScrim = functionBlock("private fun Modifier.homeHeroHeaderScrim(")

        assertTrue(dayPart.contains("else -> Color.White.copy(alpha = 0.72f)"))
        assertTrue(name.contains("color = if (LevyraIsLight) LevyraText else Color.White"))
        assertTrue(headerScrim.contains("if (isLight) 0.72f else 0.78f"))
        assertTrue(headerScrim.contains("if (isLight) 0.38f else 0.52f"))
    }

    @Test
    fun `home identity header stays transparent and compact`() {
        val greetingBar = functionBlock("private fun GreetingBar(")
        val logoBadge = functionBlock("internal fun HomeLogoBadge()")
        val greetingName = functionBlock("private fun HomeGreetingName(")

        assertTrue(greetingBar.contains(".heightIn(min = LevyraHomeDesign.SettingsControlHeight)"))
        assertTrue(logoBadge.contains(".size(LevyraHomeDesign.SettingsControlHeight)"))
        assertTrue(greetingName.contains("fontSize = 26.sp"))
        assertFalse(greetingBar.contains("cardBackground"))
        assertFalse(greetingBar.contains("LevyraHomeDesign.HeaderShape"))
        assertFalse(greetingBar.contains("text = \"LEVYRA\""))
    }

    @Test
    fun `home greeting wraps instead of truncating long names or translations`() {
        val greetingName = functionBlock("private fun HomeGreetingName(")

        assertTrue(greetingName.contains("softWrap = true"))
        assertTrue(greetingName.contains(".fillMaxWidth()"))
        assertFalse(greetingName.contains("overflow = TextOverflow.Ellipsis"))
    }

    @Test
    fun `home greeting respects reduced motion and refreshes across day parts`() {
        val greeting = functionBlock("private fun HomeGreeting(")
        val dayPart = functionBlock("private fun HomeGreetingDayPart(")
        val greetingClock = functionBlock("internal fun rememberCurrentGreetingHour()")

        assertTrue(greeting.contains("LaunchedEffect(animationsEnabled)"))
        assertTrue(greeting.contains("dayPartReveal.snapTo(1f)"))
        assertTrue(greeting.contains("nameReveal.snapTo(1f)"))
        assertTrue(dayPart.contains("targetState = dayPart"))
        assertTrue(dayPart.contains("if (animationsEnabled)"))
        assertTrue(dayPart.contains("snap()"))
        assertTrue(greetingClock.contains("java.util.Calendar.HOUR_OF_DAY"))
        assertTrue(greetingClock.contains("delay("))
    }

    @Test
    fun `compact Levyra emblem is optically enlarged inside its header tile`() {
        val logoMark = functionBlock("internal fun LevyraLogoMark(")

        assertTrue(logoMark.contains(".fillMaxSize()"))
        assertTrue(logoMark.contains("scaleX = 1.45f"))
        assertTrue(logoMark.contains("scaleY = 1.45f"))
    }

    @Test
    fun `short resonance comments are vertically balanced`() {
        val resonanceCard = functionBlock("private fun ResonanceFeaturedCard(")

        assertTrue(resonanceCard.contains(".weight(1f)"))
        assertTrue(resonanceCard.contains("contentAlignment = Alignment.CenterStart"))
        assertTrue(resonanceCard.contains("snippet.text"))
    }

    private fun functionBlock(signature: String): String {
        val start = appSource.indexOf(signature)
        require(start >= 0) { "$signature not found" }
        val bodyStart = appSource.indexOf('{', start)
        require(bodyStart > start) { "$signature body not found" }
        var depth = 0
        for (index in bodyStart until appSource.length) {
            when (appSource[index]) {
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) return appSource.substring(start, index + 1)
                }
            }
        }
        error("$signature body is not balanced")
    }

    private fun resolveRepoPath(vararg candidates: String): Path =
        candidates
            .asSequence()
            .map(Path::of)
            .firstOrNull(Files::exists)
            ?: error("None of the expected paths exist: ${candidates.joinToString()}")
}
