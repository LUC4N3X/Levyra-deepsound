package com.luc4n3x.levyra.ui.player

import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerQualityFixesTest {

    @Test
    fun `shouldTriggerFavoritePop only fires on real favorite addition on same track`() {
        // Initial composition: favorite is already true -> no pop
        assertFalse(
            shouldTriggerFavoritePop(
                previousTrackId = "track-1",
                currentTrackId = "track-1",
                wasFavorite = true,
                isFavorite = true,
                animated = true
            )
        )

        // Initial load with null previousTrackId -> no pop
        assertFalse(
            shouldTriggerFavoritePop(
                previousTrackId = null,
                currentTrackId = "track-1",
                wasFavorite = false,
                isFavorite = true,
                animated = true
            )
        )

        // User un-favorites -> no pop
        assertFalse(
            shouldTriggerFavoritePop(
                previousTrackId = "track-1",
                currentTrackId = "track-1",
                wasFavorite = true,
                isFavorite = false,
                animated = true
            )
        )

        // Track changed from track-1 to track-2 even if isFavorite is true -> no pop
        assertFalse(
            shouldTriggerFavoritePop(
                previousTrackId = "track-1",
                currentTrackId = "track-2",
                wasFavorite = false,
                isFavorite = true,
                animated = true
            )
        )

        // Animated false -> no pop
        assertFalse(
            shouldTriggerFavoritePop(
                previousTrackId = "track-1",
                currentTrackId = "track-1",
                wasFavorite = false,
                isFavorite = true,
                animated = false
            )
        )

        // Genuine favorite addition on the same track with animation enabled -> triggers pop!
        assertTrue(
            shouldTriggerFavoritePop(
                previousTrackId = "track-1",
                currentTrackId = "track-1",
                wasFavorite = false,
                isFavorite = true,
                animated = true
            )
        )
    }

    @Test
    fun `resolveTransportWeights guarantees touch target of at least 48dp on 320dp narrow screens`() {
        // 320dp width phone with 18dp gutters -> availableWidth = 284dp
        val availableWidth = 284.dp
        val gap = LevyraPlayerDesign.TransportGap // 5dp
        val totalGaps = gap * 4 // 20dp
        val availableSegmentWidth = availableWidth - totalGaps // 264dp

        val weights = resolveTransportWeights(availableWidth, gap, LevyraPlayerDesign.MinimumTouchTarget)
        val totalWeight = weights.modeWeight * 2 + weights.skipWeight * 2 + weights.playWeight

        val modeWidth = availableSegmentWidth * (weights.modeWeight / totalWeight)
        val skipWidth = availableSegmentWidth * (weights.skipWeight / totalWeight)
        val playWidth = availableSegmentWidth * (weights.playWeight / totalWeight)

        assertTrue("Mode width $modeWidth must be >= 48dp", modeWidth >= 47.9f.dp)
        assertTrue("Skip width $skipWidth must be >= 48dp", skipWidth >= 47.9f.dp)
        assertTrue("Play width $playWidth must be >= 48dp", playWidth >= 47.9f.dp)
        assertTrue("Play must remain the hero/protagonist ($playWidth > $skipWidth)", playWidth > skipWidth)
    }

    @Test
    fun `resolveTransportWeights preserves standard proportions on 360dp screens`() {
        // 360dp phone with 18dp gutters -> availableWidth = 324dp
        val availableWidth = 324.dp
        val gap = LevyraPlayerDesign.TransportGap // 5dp
        val totalGaps = gap * 4 // 20dp
        val availableSegmentWidth = availableWidth - totalGaps // 304dp

        val weights = resolveTransportWeights(availableWidth, gap, LevyraPlayerDesign.MinimumTouchTarget)
        val totalWeight = weights.modeWeight * 2 + weights.skipWeight * 2 + weights.playWeight

        val modeWidth = availableSegmentWidth * (weights.modeWeight / totalWeight)
        val skipWidth = availableSegmentWidth * (weights.skipWeight / totalWeight)
        val playWidth = availableSegmentWidth * (weights.playWeight / totalWeight)

        assertTrue("Mode width $modeWidth on 360dp must be >= 48dp", modeWidth >= 48.dp)
        assertTrue("Skip width $skipWidth on 360dp must be >= 48dp", skipWidth >= 48.dp)
        assertTrue("Play width $playWidth on 360dp must be hero", playWidth > skipWidth)
    }

    @Test
    fun `resolveTransportWeights uses standard weights on standard Pixel 8 widths`() {
        // Pixel 8 (~411dp width with 22dp gutters) -> availableWidth = 367dp
        val availableWidth = 367.dp
        val weights = resolveTransportWeights(availableWidth)

        assertEquals(0.86f, weights.modeWeight, 0.001f)
        assertEquals(1f, weights.skipWeight, 0.001f)
        assertEquals(1.56f, weights.playWeight, 0.001f)
    }

    @Test
    fun `shouldDismissSheetOnDragEnd only returns true when threshold is exceeded`() {
        val threshold = 120f
        assertFalse(shouldDismissSheetOnDragEnd(0f, threshold))
        assertFalse(shouldDismissSheetOnDragEnd(100f, threshold))
        assertFalse(shouldDismissSheetOnDragEnd(120f, threshold))
        assertTrue(shouldDismissSheetOnDragEnd(120.1f, threshold))
        assertTrue(shouldDismissSheetOnDragEnd(200f, threshold))
    }

    @Test
    fun `visual mode accessibility semantics describe current mode without boolean toggle`() {
        val strings = LevyraStrings.forCode("en")

        assertEquals("Visual mode", visualModeLabel(strings))
        assertEquals("Artwork", visualModeStateDescription(PlayerVisualMode.Artwork, strings))
        assertEquals("Canvas card", visualModeStateDescription(PlayerVisualMode.CanvasCard, strings))
        assertEquals("Canvas immersive", visualModeStateDescription(PlayerVisualMode.CanvasImmersive, strings))

        val italianStrings = LevyraStrings.forCode("it")
        assertEquals("Modalità visiva", visualModeLabel(italianStrings))
        assertEquals("Copertina", visualModeStateDescription(PlayerVisualMode.Artwork, italianStrings))
        assertEquals("Scheda Canvas", visualModeStateDescription(PlayerVisualMode.CanvasCard, italianStrings))
        assertEquals("Canvas immersivo", visualModeStateDescription(PlayerVisualMode.CanvasImmersive, italianStrings))
    }
}
