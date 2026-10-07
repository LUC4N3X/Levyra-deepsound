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
    fun `personal orbit keeps every selected track in compact paged access`() {
        val shelf = functionBlock("private fun PersonalListeningShelf(")

        assertTrue(shelf.contains("homePersonalOrbitPages"))
        assertTrue(shelf.contains("PersonalOrbitTile"))
        assertTrue(shelf.contains("HorizontalPager"))
        assertTrue(shelf.contains("FEED_ACCESS_COLUMNS"))
        assertTrue(shelf.contains("OrbitWallMinWidth"))
        assertTrue(shelf.contains("onPlayAll"))
        assertTrue(shelf.contains("onTrackActions"))
        assertFalse(shelf.contains("HomeTrackRow"))
        assertFalse(shelf.contains("LazyRow"))
        assertFalse(shelf.contains("columnIndex"))
    }

    @Test
    fun `orbit tiles keep playback actions and readable compact titles`() {
        val tile = functionBlock("private fun PersonalOrbitTile(")

        assertTrue(tile.contains("heightIn(min = 64.dp)"))
        assertTrue(tile.contains("levyraPressable("))
        assertTrue(tile.contains("onLongClick = onActions"))
        assertTrue(tile.contains("strings.songOptions"))
        assertTrue(tile.contains("maxLines = 2"))
        assertTrue(tile.contains("HomeNowPlayingScrim"))
    }

    @Test
    fun `orbit header shows the listener only when a name is set`() {
        val header = functionBlock("private fun HomeOrbitHeader(")

        assertTrue(header.contains("homePersonalOrbitInitial"))
        assertTrue(header.contains("if (initial != null)"))
        assertTrue(header.indexOf("if (initial != null)") < header.indexOf("HomeOrbitAvatar("))
        assertTrue(header.contains("personalOrbitTitle"))
        assertTrue(header.contains("heading()"))
        assertTrue(header.contains("HomeOutlinedAction"))
        assertFalse(header.contains("personalOrbitSubtitle"))
    }

    @Test
    fun `orbit avatar prefers the profile photo over the initial`() {
        val avatar = functionBlock("private fun HomeOrbitAvatar(")

        assertTrue(avatar.contains("photoPath.takeIf(String::isNotBlank)"))
        assertTrue(avatar.contains("AsyncImage("))
        assertTrue(avatar.contains("ContentScale.Crop"))
        assertTrue(avatar.contains("CachePolicy.DISABLED"))
        assertTrue(avatar.indexOf("text = initial") < avatar.indexOf("AsyncImage("))
    }

    @Test
    fun `compact access leaves room for artwork and readable titles on phones`() {
        val tileWidth = (
            360f - LevyraHomeDesign.HorizontalInset.value -
                LevyraHomeDesign.OrbitPageEndInset.value - LevyraHomeDesign.OrbitTileGap.value
            ) / LevyraHomeDesign.FEED_ACCESS_COLUMNS
        val textWidth = tileWidth - 12f - 48f - 8f
        assertTrue(textWidth >= 80f)
        assertTrue(LevyraHomeDesign.FEED_ACCESS_PAGE_SIZE % LevyraHomeDesign.FEED_ACCESS_COLUMNS == 0)
    }

    @Test
    fun `compact access stays readable on wide windows`() {
        val tileWidth = (
            LevyraHomeDesign.OrbitWallPageWidth.value - LevyraHomeDesign.HorizontalInset.value -
                LevyraHomeDesign.OrbitPageEndInset.value - LevyraHomeDesign.OrbitTileGap.value
            ) / LevyraHomeDesign.FEED_ACCESS_COLUMNS
        assertTrue(tileWidth - 12f - 48f - 8f >= 80f)
        assertTrue(LevyraHomeDesign.OrbitWallPageWidth < LevyraHomeDesign.OrbitWallMinWidth)
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
