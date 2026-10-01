package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePremiumLayoutContractTest {
    private val source: String by lazy {
        val path = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
        ).firstOrNull(Files::exists) ?: error("LevyraApp.kt not found")
        Files.readString(path)
    }

    @Test
    fun `personal orbit is a compact horizontal quick shelf`() {
        val shelf = functionBlock("private fun PersonalListeningShelf(")

        assertTrue(shelf.contains("LazyRow"))
        assertTrue(shelf.contains("homePersonalOrbitColumns"))
        assertTrue(shelf.contains("HomeTrackRow"))
        assertTrue(shelf.contains("onPlayAll"))
        assertTrue(shelf.contains("onTrackActions"))
        assertFalse(shelf.contains("HorizontalPager"))
        assertFalse(shelf.contains("SPEED_DIAL_COLUMNS"))
        assertFalse(shelf.contains("PersonalOrbitSpeedDialCard"))
    }

    @Test
    fun `orbit rows keep accessible compact touch targets`() {
        assertTrue(LevyraHomeDesign.TrackRowHeight.value >= 48f)
        assertTrue(LevyraHomeDesign.TrackThumbSize < LevyraHomeDesign.TrackRowHeight)
        assertTrue(LevyraHomeDesign.TrackColumnPeek.value > 0f)
    }

    private fun functionBlock(signature: String): String {
        val start = source.indexOf(signature)
        require(start >= 0) { "$signature not found" }
        val bodyStart = source.indexOf('{', start)
        require(bodyStart > start) { "$signature body not found" }
        var depth = 0
        for (index in bodyStart until source.length) {
            when (source[index]) {
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) return source.substring(start, index + 1)
                }
            }
        }
        error("$signature body is not balanced")
    }
}
