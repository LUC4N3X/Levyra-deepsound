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
    fun `personal orbit is a paged three by three artwork grid`() {
        val shelf = functionBlock("private fun PersonalListeningShelf(")

        assertTrue(shelf.contains("homePersonalOrbitPages"))
        assertTrue(shelf.contains("PersonalOrbitTile"))
        assertTrue(shelf.contains("HorizontalPager"))
        assertTrue(shelf.contains("HOME_PERSONAL_ORBIT_GRID_COLUMNS"))
        assertTrue(shelf.contains("OrbitWallMinWidth"))
        assertTrue(shelf.contains("onPlayAll"))
        assertTrue(shelf.contains("onTrackActions"))
        assertFalse(shelf.contains("HomeTrackRow"))
        assertFalse(shelf.contains("LazyRow"))
        assertFalse(shelf.contains("columnIndex"))
    }

    @Test
    fun `orbit tiles keep playback actions and a single line title`() {
        val tile = functionBlock("private fun PersonalOrbitTile(")

        assertTrue(tile.contains("aspectRatio(1f)"))
        assertTrue(tile.contains("levyraPressable("))
        assertTrue(tile.contains("onLongClick = onActions"))
        assertTrue(tile.contains("strings.songOptions"))
        assertTrue(tile.contains("maxLines = 1"))
        assertTrue(tile.contains("ActiveTrackEqualizer"))
    }

    @Test
    fun `orbit header shows the listener and keeps play all`() {
        val header = functionBlock("private fun HomeOrbitHeader(")

        assertTrue(header.contains("homePersonalOrbitInitial"))
        assertTrue(header.contains("personalOrbitTitle"))
        assertTrue(header.contains("heading()"))
        assertTrue(header.contains("HomeOutlinedAction"))
        assertFalse(header.contains("personalOrbitSubtitle"))
    }

    @Test
    fun `orbit grid proportions stay readable on wide windows`() {
        val wallTile = (
            LevyraHomeDesign.OrbitWallPageWidth.value -
                LevyraHomeDesign.HorizontalInset.value * 2 -
                LevyraHomeDesign.OrbitTileGap.value * 2
            ) / 3f

        assertTrue(wallTile >= 96f)
        assertTrue(LevyraHomeDesign.OrbitWallPageWidth < LevyraHomeDesign.OrbitWallMinWidth)
        assertTrue(LevyraHomeDesign.OrbitTileTitleSize.value >= 14f)
        assertTrue(LevyraHomeDesign.OrbitAvatarSize.value >= 32f)
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
