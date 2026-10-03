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
        val greetingBar = functionBlock("private fun GreetingBar(")
        val headerScrim = functionBlock("private fun Modifier.homeHeroHeaderScrim(")

        assertTrue(
            greetingBar.contains(
                "color = if (LevyraIsLight) LevyraMuted else Color.White.copy(alpha = 0.92f)"
            )
        )
        assertTrue(headerScrim.contains("if (isLight) 0.94f else 0.78f"))
        assertTrue(headerScrim.contains("if (isLight) 0.78f else 0.52f"))
    }

    @Test
    fun `home greeting wraps instead of truncating long names or translations`() {
        val greetingBar = functionBlock("private fun GreetingBar(")

        assertTrue(greetingBar.contains("softWrap = true"))
        assertTrue(greetingBar.contains("modifier = Modifier.fillMaxWidth()"))
        assertFalse(greetingBar.contains("overflow = TextOverflow.Ellipsis"))
    }

    @Test
    fun `compact Levyra emblem is optically enlarged inside its header tile`() {
        val logoMark = functionBlock("private fun LevyraLogoMark(")

        assertTrue(logoMark.contains(".fillMaxSize()"))
        assertTrue(logoMark.contains("scaleX = 1.45f"))
        assertTrue(logoMark.contains("scaleY = 1.45f"))
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
