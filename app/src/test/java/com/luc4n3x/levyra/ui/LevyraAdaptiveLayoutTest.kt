package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraAdaptiveLayoutTest {

    @Test
    fun `phones stay compact and tablets become expanded`() {
        assertEquals(LevyraLayoutMode.Compact, resolveLevyraLayoutMode(392f, 850f))
        assertEquals(LevyraLayoutMode.Medium, resolveLevyraLayoutMode(673f, 841f))
        assertEquals(LevyraLayoutMode.Expanded, resolveLevyraLayoutMode(1280f, 800f))
    }

    @Test
    fun `an unfolded inner display crosses into medium`() {
        assertEquals(LevyraLayoutMode.Compact, resolveLevyraLayoutMode(374f, 819f))
        assertEquals(LevyraLayoutMode.Medium, resolveLevyraLayoutMode(674f, 841f))
    }

    @Test
    fun `a short landscape phone is treated as medium`() {
        assertEquals(LevyraLayoutMode.Medium, resolveLevyraLayoutMode(800f, 392f))
        assertEquals(LevyraLayoutMode.Expanded, resolveLevyraLayoutMode(850f, 392f))
    }

    @Test
    fun `the player splits into two panes only when it fits`() {
        assertEquals(LevyraPlayerPane.Stacked, resolvePlayerPane(392f, 850f))
        assertEquals(LevyraPlayerPane.Stacked, resolvePlayerPane(673f, 841f))
        assertEquals(LevyraPlayerPane.SideBySide, resolvePlayerPane(1280f, 800f))
        assertEquals(LevyraPlayerPane.SideBySide, resolvePlayerPane(850f, 392f))
    }

    @Test
    fun `a very short window keeps the stacked player`() {
        assertEquals(LevyraPlayerPane.Stacked, resolvePlayerPane(900f, 300f))
    }

    @Test
    fun `content widens with the window without becoming unbounded`() {
        assertEquals(560f, levyraContentMaxWidthDp(LevyraLayoutMode.Compact), 0.0001f)
        assertEquals(720f, levyraContentMaxWidthDp(LevyraLayoutMode.Medium), 0.0001f)
        assertEquals(1080f, levyraContentMaxWidthDp(LevyraLayoutMode.Expanded), 0.0001f)
    }

    @Test
    fun `the two pane artwork stays smaller than the stacked artwork`() {
        val sideBySide = levyraPlayerArtworkMaxWidthDp(LevyraPlayerPane.SideBySide, LevyraLayoutMode.Expanded)
        val stacked = levyraPlayerArtworkMaxWidthDp(LevyraPlayerPane.Stacked, LevyraLayoutMode.Expanded)
        assertTrue(sideBySide < stacked)
    }

    @Test
    fun `the mini player is unbounded on phones and capped on large screens`() {
        assertFalse(levyraMiniPlayerMaxWidthDp(LevyraLayoutMode.Compact).isFinite())
        assertEquals(640f, levyraMiniPlayerMaxWidthDp(LevyraLayoutMode.Medium), 0.0001f)
        assertEquals(760f, levyraMiniPlayerMaxWidthDp(LevyraLayoutMode.Expanded), 0.0001f)
    }

    @Test
    fun `gutters grow with the window`() {
        assertEquals(18f, levyraFoldAwareGutterDp(LevyraLayoutMode.Compact, compactPlayer = true), 0.0001f)
        assertEquals(22f, levyraFoldAwareGutterDp(LevyraLayoutMode.Compact, compactPlayer = false), 0.0001f)
        assertEquals(28f, levyraFoldAwareGutterDp(LevyraLayoutMode.Medium, compactPlayer = false), 0.0001f)
        assertEquals(34f, levyraFoldAwareGutterDp(LevyraLayoutMode.Expanded, compactPlayer = true), 0.0001f)
    }
}
