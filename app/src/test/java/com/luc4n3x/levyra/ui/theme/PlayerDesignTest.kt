package com.luc4n3x.levyra.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDesignTest {
    @Test
    fun primaryTransportActionRemainsDominantAndCircular() {
        assertEquals(LevyraPlayerDesign.PrimaryWidth, LevyraPlayerDesign.PrimaryHeight)
        assertEquals(
            LevyraPlayerDesign.PrimaryWidthCompact,
            LevyraPlayerDesign.PrimaryHeightCompact
        )
        assertTrue(
            LevyraPlayerDesign.PrimaryWidth.value > LevyraPlayerDesign.TransportButton.value
        )
        assertTrue(
            LevyraPlayerDesign.PrimaryWidthCompact.value >
                LevyraPlayerDesign.TransportButtonCompact.value
        )
    }

    @Test
    fun playerSurfaceHierarchyStaysOrdered() {
        assertTrue(
            LevyraPlayerDesign.GlassFillRaised.alpha >
                LevyraPlayerDesign.GlassFillStrong.alpha
        )
        assertTrue(
            LevyraPlayerDesign.GlassFillStrong.alpha >
                LevyraPlayerDesign.GlassFill.alpha
        )
        assertTrue(
            LevyraPlayerDesign.CornerXl.value > LevyraPlayerDesign.CornerLg.value
        )
        assertTrue(
            LevyraPlayerDesign.CornerLg.value > LevyraPlayerDesign.CornerMd.value
        )
    }

    @Test
    fun scrubStateExpandsTrackAndThumb() {
        assertTrue(
            LevyraPlayerDesign.TrackHeightActive.value >
                LevyraPlayerDesign.TrackHeight.value
        )
        assertTrue(
            LevyraPlayerDesign.HandleWidthActive.value >
                LevyraPlayerDesign.HandleWidth.value
        )
        assertEquals(
            LevyraPlayerDesign.HandleWidth,
            LevyraPlayerDesign.HandleHeight
        )
        assertEquals(
            LevyraPlayerDesign.HandleWidthActive,
            LevyraPlayerDesign.HandleHeightActive
        )
    }
}
