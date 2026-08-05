package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraExperiencePolicyTest {

    @Test
    fun `small phones use compact player layout`() {
        assertEquals(LevyraPlayerLayout.Compact, levyraPlayerLayout(widthDp = 360f, heightDp = 640f))
    }

    @Test
    fun `normal portrait phones use portrait player layout`() {
        assertEquals(LevyraPlayerLayout.Portrait, levyraPlayerLayout(widthDp = 412f, heightDp = 892f))
    }

    @Test
    fun `tablets and unfolded devices use expanded player layout`() {
        assertEquals(LevyraPlayerLayout.Expanded, levyraPlayerLayout(widthDp = 840f, heightDp = 1_100f))
        assertEquals(LevyraPlayerLayout.Expanded, levyraPlayerLayout(widthDp = 720f, heightDp = 600f))
    }

    @Test
    fun `dominant horizontal swipes change tracks`() {
        assertEquals(LevyraPlayerGesture.Previous, levyraPlayerGesture(90f, 12f, 60f))
        assertEquals(LevyraPlayerGesture.Next, levyraPlayerGesture(-90f, 12f, 60f))
    }

    @Test
    fun `dominant vertical swipes expand and collapse`() {
        assertEquals(LevyraPlayerGesture.Expand, levyraPlayerGesture(8f, -90f, 60f))
        assertEquals(LevyraPlayerGesture.Collapse, levyraPlayerGesture(8f, 90f, 60f))
    }

    @Test
    fun `small movement is ignored`() {
        assertEquals(LevyraPlayerGesture.None, levyraPlayerGesture(20f, -18f, 60f))
    }

    @Test
    fun `lyrics choose exact line then nearest previous line`() {
        val lyrics = listOf(
            LyricLine(startMs = 0L, endMs = 999L, text = "one"),
            LyricLine(startMs = 1_500L, endMs = 2_400L, text = "two"),
            LyricLine(startMs = 3_000L, endMs = 4_000L, text = "three")
        )

        assertEquals(1, immersiveActiveLyricIndex(1_900L, lyrics))
        assertEquals(1, immersiveActiveLyricIndex(2_700L, lyrics))
        assertEquals(-1, immersiveActiveLyricIndex(-10L, lyrics))
    }
}
