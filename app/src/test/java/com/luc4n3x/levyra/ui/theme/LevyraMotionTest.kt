package com.luc4n3x.levyra.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraMotionTest {

    private val tokens = listOf(
        LevyraMotion.press,
        LevyraMotion.release,
        LevyraMotion.expand,
        LevyraMotion.collapse,
        LevyraMotion.settle,
        LevyraMotion.snappy,
        LevyraMotion.expressive,
        LevyraMotion.reorder
    )

    @Test
    fun `spring tokens are deterministic and physically sane`() {
        tokens.forEach { token ->
            assertTrue("$token damping", token.dampingRatio in 0.5f..1f)
            assertTrue("$token stiffness", token.stiffness in 200f..1_500f)
            val first = token.spec<Float>()
            val second = token.spec<Float>()
            assertEquals(first, second)
            assertEquals(token.dampingRatio, first.dampingRatio, 0f)
            assertEquals(token.stiffness, first.stiffness, 0f)
        }
    }

    @Test
    fun `only feedback springs are allowed to bounce noticeably`() {
        val bouncy = tokens.filter { it.dampingRatio < 0.7f }
        assertEquals(setOf(LevyraMotion.release, LevyraMotion.expressive), bouncy.toSet())
        assertTrue(LevyraMotion.settle.dampingRatio >= 0.9f)
        assertTrue(LevyraMotion.collapse.dampingRatio > LevyraMotion.expand.dampingRatio)
    }

    @Test
    fun `durations are ordered and short enough for a music app`() {
        val durations = with(LevyraMotion.Durations) { listOf(Instant, Quick, Short, Medium, Long, Palette) }
        assertEquals(durations.sorted(), durations)
        assertTrue(durations.all { it in 1..700 })
    }

    @Test
    fun `reduced motion snaps every spec`() {
        assertTrue(LevyraMotion.spec(false, LevyraMotion.crossfade<Float>()) is SnapSpec)
        assertTrue(LevyraMotion.physics<Float>(false, LevyraMotion.press) is SnapSpec)
        assertTrue(LevyraMotion.physics<Float>(true, LevyraMotion.press) is SpringSpec)
        assertTrue(LevyraMotion.spec(true, LevyraMotion.crossfade<Float>()) is TweenSpec)
        assertSame(EnterTransition.None, LevyraMotion.overlayEnter(false))
        assertSame(ExitTransition.None, LevyraMotion.overlayExit(false))
        assertSame(EnterTransition.None, LevyraMotion.sheetEnter(false))
        assertSame(ExitTransition.None, LevyraMotion.sheetExit(false))
        assertNotEquals(EnterTransition.None, LevyraMotion.overlayEnter(true))
        val reduced = LevyraMotion.contentSwap(false)
        assertSame(EnterTransition.None, reduced.targetContentEnter)
        assertSame(ExitTransition.None, reduced.initialContentExit)
    }

    @Test
    fun `time based specs keep their duration contract`() {
        assertEquals(LevyraMotion.Durations.Medium, LevyraMotion.crossfade<Float>().durationMillis)
        assertEquals(LevyraMotion.Durations.Long, LevyraMotion.artwork<Float>().durationMillis)
        assertEquals(LevyraMotion.Durations.Palette, LevyraMotion.palette<Float>().durationMillis)
        assertEquals(LevyraMotion.Durations.Short, LevyraMotion.fade<Float>().durationMillis)
        assertEquals(0, LevyraMotion.fade<Float>().delay)
    }

    @Test
    fun `player design delegates to the shared motion language without changing values`() {
        assertEquals(0.64f, LevyraPlayerDesign.ExpressiveDamping, 0f)
        assertEquals(360f, LevyraPlayerDesign.ExpressiveStiffness, 0f)
        assertEquals(0.90f, LevyraPlayerDesign.SmoothDamping, 0f)
        assertEquals(540f, LevyraPlayerDesign.SmoothStiffness, 0f)
        assertEquals(0.58f, LevyraPlayerDesign.PressDamping, 0f)
        assertEquals(760f, LevyraPlayerDesign.PressStiffness, 0f)
        assertEquals(650, LevyraPlayerDesign.PaletteMillis)
        assertEquals(LevyraMotion.expressive.spec<Float>(), LevyraPlayerDesign.expressiveSpring<Float>())
        assertEquals(LevyraMotion.settle.spec<Float>(), LevyraPlayerDesign.smoothSpring<Float>())
        assertEquals(LevyraMotion.release.spec<Float>(), LevyraPlayerDesign.pressSpring<Float>())
        assertEquals(320, LevyraPlayerDesign.emphasizedTween<Float>().durationMillis)
        assertEquals(220, LevyraPlayerDesign.standardTween<Float>().durationMillis)
        assertTrue(LevyraPlayerDesign.motion(false, LevyraPlayerDesign.paletteTween<Float>()) is SnapSpec)
    }

    @Test
    fun `press scales come from one place`() {
        assertEquals(LevyraMotion.Scale.Row, LevyraPressScale.Row, 0f)
        assertEquals(LevyraMotion.Scale.Tile, LevyraPressScale.Tile, 0f)
        assertEquals(LevyraMotion.Scale.Surface, LevyraPressScale.Surface, 0f)
        assertEquals(LevyraMotion.Scale.Control, LevyraPressScale.Control, 0f)
        listOf(LevyraPressScale.Row, LevyraPressScale.Tile, LevyraPressScale.Surface, LevyraPressScale.Control).forEach {
            assertTrue(it in 0.9f..1f)
        }
    }
}
