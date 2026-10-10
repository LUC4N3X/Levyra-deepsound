package com.luc4n3x.levyra.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraAdaptiveDockTest {

    @Test
    fun `dock fade computes expected alpha at key milestones`() {
        assertEquals(1f, dockFade(0f), 0.001f)
        assertEquals(0.5f, dockFade(0.3f), 0.001f)
        assertEquals(0f, dockFade(0.6f), 0.001f)
        assertEquals(0f, dockFade(1f), 0.001f)
        assertEquals(1f, dockFade(-0.2f), 0.001f)
    }

    @Test
    fun `dock state starts expanded`() {
        val state = LevyraDockState(collapseDistancePx = 100f, expandDistancePx = 50f)
        assertFalse(state.compact)
    }

    @Test
    fun `scrolling up past collapse distance triggers compact`() {
        val state = LevyraDockState(collapseDistancePx = 100f, expandDistancePx = 50f)

        // Scroll up by 40px (consumed.y = -40f) -> not yet compact
        state.onPostScroll(
            consumed = Offset(0f, -40f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput
        )
        assertFalse(state.compact)

        // Scroll up by another 70px -> total 110px >= 100px collapse distance -> compact!
        state.onPostScroll(
            consumed = Offset(0f, -70f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput
        )
        assertTrue(state.compact)
    }

    @Test
    fun `reversing scroll direction resets travel distance`() {
        val state = LevyraDockState(collapseDistancePx = 100f, expandDistancePx = 50f)

        // Scroll up 80px
        state.onPostScroll(
            consumed = Offset(0f, -80f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput
        )
        assertFalse(state.compact)

        // Scroll down 10px (reversal)
        state.onPostScroll(
            consumed = Offset(0f, 10f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput
        )
        assertFalse(state.compact)

        // Scroll up 30px -> should NOT compact because reversal reset travel
        state.onPostScroll(
            consumed = Offset(0f, -30f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput
        )
        assertFalse(state.compact)
    }

    @Test
    fun `scrolling down past expand distance expands compact dock`() {
        val state = LevyraDockState(collapseDistancePx = 100f, expandDistancePx = 50f)

        // Compact first
        state.onPostScroll(
            consumed = Offset(0f, -120f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput
        )
        assertTrue(state.compact)

        // Scroll down by 30px -> not yet expanded
        state.onPostScroll(
            consumed = Offset(0f, 30f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput
        )
        assertTrue(state.compact)

        // Scroll down by another 25px -> total 55px >= 50px -> expanded!
        state.onPostScroll(
            consumed = Offset(0f, 25f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput
        )
        assertFalse(state.compact)
    }

    @Test
    fun `overscroll available downwards expands immediately`() {
        val state = LevyraDockState(collapseDistancePx = 100f, expandDistancePx = 50f)

        // Compact first
        state.onPostScroll(
            consumed = Offset(0f, -120f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput
        )
        assertTrue(state.compact)

        // Reaching the top of the list produces available.y > 0
        state.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 15f),
            source = NestedScrollSource.UserInput
        )
        assertFalse(state.compact)
    }

    @Test
    fun `compaction is prohibited when compactionAllowed is false`() {
        val state = LevyraDockState(collapseDistancePx = 100f, expandDistancePx = 50f)
        state.compactionAllowed = false

        state.onPostScroll(
            consumed = Offset(0f, -200f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput
        )
        assertFalse(state.compact)
    }

    @Test
    fun `explicit expand resets state`() {
        val state = LevyraDockState(collapseDistancePx = 100f, expandDistancePx = 50f)

        state.onPostScroll(
            consumed = Offset(0f, -150f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput
        )
        assertTrue(state.compact)

        state.expand()
        assertFalse(state.compact)
    }
}
