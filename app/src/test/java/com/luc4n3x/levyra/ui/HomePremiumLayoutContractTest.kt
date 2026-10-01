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

    @Test
    fun `orbit rows follow the quick picks proportions`() {
        assertTrue(LevyraHomeDesign.OrbitThumbSize.value >= 56f)
        assertTrue(LevyraHomeDesign.OrbitThumbSize < LevyraHomeDesign.OrbitRowHeight)
        assertTrue(LevyraHomeDesign.OrbitRowHeight.value >= 48f)
        assertTrue(LevyraHomeDesign.OrbitTitleSize.value > LevyraHomeDesign.OrbitSubtitleSize.value)
        assertTrue(LevyraHomeDesign.OrbitSubtitleSize.value >= 13f)
        assertTrue(LevyraHomeDesign.OrbitColumnPeek.value in 12f..28f)
        assertTrue(LevyraHomeDesign.OrbitColumnMaxWidth.value >= 360f)
    }

    @Test
    fun `orbit shelf applies the orbit proportions and keeps a single line header`() {
        val shelf = functionBlock("private fun PersonalListeningShelf(")
        assertTrue(shelf.contains("OrbitThumbSize"))
        assertTrue(shelf.contains("OrbitRowHeight"))
        assertTrue(shelf.contains("OrbitTitleSize"))
        assertTrue(shelf.contains("OrbitColumnPeek"))
        assertTrue(shelf.contains("OrbitColumnMaxWidth"))
        assertFalse(shelf.contains("HOME_DENSE_SHELF_MAX_WIDTH"))

        val header = functionBlock("private fun HomeOrbitHeader(")
        assertTrue(header.contains("personalOrbitTitle"))
        assertTrue(header.contains("HomeOutlinedAction"))
        assertFalse(header.contains("personalOrbitSubtitle"))
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
