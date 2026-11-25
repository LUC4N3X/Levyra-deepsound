package com.luc4n3x.levyra.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TurntableGeometryTest {

    @Test
    fun `the stylus lands on the outer groove at the start and the inner groove at the end`() {
        val start = TurntableGeometry.armAngleForProgress(0f)
        val end = TurntableGeometry.armAngleForProgress(1f)
        assertEquals(TurntableGeometry.OuterGroove, TurntableGeometry.stylusRadius(start), 0.002f)
        assertEquals(TurntableGeometry.InnerGroove, TurntableGeometry.stylusRadius(end), 0.002f)
        assertTrue(end > start)
    }

    @Test
    fun `the resting arm sits outside the seek ring`() {
        assertTrue(TurntableGeometry.stylusRadius(TurntableGeometry.RestAngle) > TurntableGeometry.RingRadius)
    }

    @Test
    fun `ring fractions run clockwise from twelve o clock`() {
        val side = 1000f
        val cx = TurntableGeometry.CenterX * side
        val cy = TurntableGeometry.CenterY * side
        assertEquals(0f, TurntableGeometry.fractionAt(cx, cy - 400f, side), 0.001f)
        assertEquals(0.25f, TurntableGeometry.fractionAt(cx + 400f, cy, side), 0.001f)
        assertEquals(0.5f, TurntableGeometry.fractionAt(cx, cy + 400f, side), 0.001f)
        assertEquals(0.75f, TurntableGeometry.fractionAt(cx - 400f, cy, side), 0.001f)
    }

    @Test
    fun `a drag that starts just before twelve and crosses it seeks from the beginning`() {
        val start = TurntableGeometry.startingFraction(0.99f)
        val crossed = start + TurntableGeometry.shortestDelta(0.99f, 0.03f)
        assertEquals(0.03f, crossed.coerceIn(0f, 1f), 0.0001f)
    }

    @Test
    fun `dragging past the end stays at the end until the finger comes back`() {
        var unwrapped = TurntableGeometry.startingFraction(0.9f)
        unwrapped += TurntableGeometry.shortestDelta(0.9f, 0.05f)
        assertEquals(1f, unwrapped.coerceIn(0f, 1f), 0.0001f)
        unwrapped += TurntableGeometry.shortestDelta(0.05f, 0.8f)
        assertEquals(0.8f, unwrapped.coerceIn(0f, 1f), 0.0001f)
    }

    @Test
    fun `only touches on the ring band start a seek`() {
        val side = 1000f
        val cx = TurntableGeometry.CenterX * side
        val cy = TurntableGeometry.CenterY * side
        assertTrue(TurntableGeometry.inSeekBand(cx, cy - 425f, side))
        assertFalse(TurntableGeometry.inSeekBand(cx, cy - 200f, side))
        assertFalse(TurntableGeometry.inSeekBand(cx, cy - 560f, side))
    }
}
