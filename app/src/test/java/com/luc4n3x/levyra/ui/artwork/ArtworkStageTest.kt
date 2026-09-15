package com.luc4n3x.levyra.ui.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkStageTest {

    @Test
    fun dissolveKeepsArtworkOpaqueUntilFadeStartsAndEndsTransparent() {
        val stops = artworkDissolveStops(0.36f)

        assertEquals(0f, stops.first().first, 0f)
        assertEquals(1f, stops.first().second.alpha, 0f)
        assertEquals(1f - 0.36f, stops[1].first, 0.0001f)
        assertEquals(1f, stops[1].second.alpha, 0.0001f)
        assertEquals(1f, stops.last().first, 0.0001f)
        assertEquals(0f, stops.last().second.alpha, 0.0001f)
    }

    @Test
    fun dissolveIsMonotonicWithoutHardSteps() {
        val stops = artworkDissolveStops(0.40f)

        stops.toList().zipWithNext().forEach { (previous, next) ->
            assertTrue(next.first >= previous.first)
            assertTrue(next.second.alpha <= previous.second.alpha)
            assertTrue(previous.second.alpha - next.second.alpha <= 0.2f)
        }
    }

    @Test
    fun coldLoadKeepsFallbackVisibleBeneathUntilArtworkIsReady() {
        assertEquals(SeamlessFallbackPlacement.Beneath, seamlessFallbackPlacement(SeamlessArtworkPhase.Loading, bridged = false))
        assertEquals(SeamlessFallbackPlacement.Beneath, seamlessFallbackPlacement(SeamlessArtworkPhase.Stalled, bridged = false))
        assertEquals(SeamlessFallbackPlacement.Hidden, seamlessFallbackPlacement(SeamlessArtworkPhase.Ready, bridged = false))
        assertEquals(SeamlessFallbackPlacement.Above, seamlessFallbackPlacement(SeamlessArtworkPhase.Failed, bridged = false))
    }

    @Test
    fun staleBridgeIsCoveredOnceLoadingStalls() {
        assertEquals(SeamlessFallbackPlacement.Beneath, seamlessFallbackPlacement(SeamlessArtworkPhase.Loading, bridged = true))
        assertEquals(SeamlessFallbackPlacement.Above, seamlessFallbackPlacement(SeamlessArtworkPhase.Stalled, bridged = true))
    }

    @Test
    fun dissolveFractionIsClamped() {
        assertEquals(0.95f, artworkDissolveStops(0f)[1].first, 0.0001f)
        assertEquals(0f, artworkDissolveStops(4f)[1].first, 0.0001f)
    }
}
