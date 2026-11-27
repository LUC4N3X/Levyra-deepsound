package com.luc4n3x.levyra.ui

import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.NavIndicator
import com.luc4n3x.levyra.domain.NavLabelMode
import com.luc4n3x.levyra.domain.NavStyle
import com.luc4n3x.levyra.domain.NavTuning
import com.luc4n3x.levyra.ui.theme.levyraLineHeightSp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationLabelLayoutTest {
    @Test
    fun `content clearance uses the rendered dock height after text scaling`() {
        val tuning = NavTuning(heightDp = 64)
        val geometry = navDockGeometry(true, true, tuning.heightDp.dp, 50.dp)
        assertEquals(20.dp, navDockExtraClearance(NavStyle.FloatingDock, tuning, geometry.height))
        assertEquals(0.dp, navDockExtraClearance(NavStyle.Original, tuning, geometry.height))
    }

    @Test
    fun `label room contains typography and the icon gap at every text scale`() {
        for (scale in listOf(1f, 1.3f, 1.5f, 2f, 3f)) {
            for (mode in listOf(NavLabelMode.Always, NavLabelMode.ActiveOnly, NavLabelMode.Compact)) {
                val font = if (mode == NavLabelMode.Compact) 10f else 11f
                val line = (levyraLineHeightSp(font) * scale).dp
                val gap = if (mode == NavLabelMode.Compact) 0.dp else 2.dp
                val room = navLabelRoom(mode, line)
                assertTrue("$mode at $scale", room >= line + gap)
            }
        }
    }

    @Test
    fun `minimum height leaves both edges clear of icons and labels`() {
        for (requested in listOf(48, 56, 64, 80)) {
            for (room in listOf(16.dp, 26.dp, 34.dp, 50.dp)) {
                val geometry = navDockGeometry(true, true, requested.dp, room)
                assertTrue(geometry.iconTop >= 4.dp)
                assertTrue(geometry.iconTop + 32.dp + room + 4.dp <= geometry.height)
                assertTrue(geometry.compactHeight >= 48.dp)
            }
        }
    }

    @Test
    fun `dot indicator stays inside the dock below the label`() {
        for (scale in listOf(0.6f, 1f, 1.6f)) {
            val dotRoom = 5.dp + 2.5.dp * scale
            for (labelRoom in listOf(0.dp, 14.5.dp, 18.dp, 34.dp, 50.dp)) {
                val geometry = navDockGeometry(true, true, 64.dp, labelRoom, dotRoom)
                val dotBottom = geometry.iconTop + 32.dp + labelRoom + dotRoom
                assertTrue(dotBottom + 4.dp <= geometry.height)
            }
        }
    }

    @Test
    fun `regular dock proportions and collapsed footprint stay unchanged when labels fit`() {
        val geometry = navDockGeometry(true, true, 64.dp, 18.dp)
        assertEquals(64.dp, geometry.height)
        assertEquals(48.dp, geometry.compactHeight)
        assertEquals(7.dp, geometry.iconTop)
    }

    @Test
    fun `collapsed tray without stacked labels keeps icons centered in the visible band`() {
        for (requested in listOf(56, 60, 80)) {
            val geometry = navDockGeometry(false, true, requested.dp, 0.dp)
            val visibleIconCenter = geometry.iconTop - geometry.lift + 16.dp
            assertEquals(geometry.compactHeight / 2, visibleIconCenter)
        }
    }

    @Test
    fun `collapsed tray with stacked labels keeps sliding labels out`() {
        val geometry = navDockGeometry(false, true, 76.dp, 18.dp)
        assertEquals(0.dp, geometry.lift)
    }

    @Test
    fun `only floating expanding pill docks squeeze sideways when collapsed`() {
        val geometry = navDockGeometry(true, true, 64.dp, 0.dp)
        val pill = NavTuning(labels = NavLabelMode.Expanding, indicator = NavIndicator.Pill)
        val floatingPill = pill.copy(indicator = NavIndicator.Floating)
        assertEquals(8.dp, navCompactionSqueeze(true, navExpansion(NavStyle.FloatingDock), pill, geometry))
        assertEquals(0.dp, navCompactionSqueeze(true, navExpansion(NavStyle.LiquidGlass), floatingPill, geometry))
        assertEquals(0.dp, navCompactionSqueeze(true, null, pill, geometry))
        assertEquals(0.dp, navCompactionSqueeze(false, navExpansion(NavStyle.SuperExpressive), pill, geometry))
    }

    @Test
    fun `dock squeeze follows compaction and yields to an attached mini player`() {
        assertEquals(0, navSqueezeSide(21f, 0f, 0f))
        assertEquals(11, navSqueezeSide(21f, 0.5f, 0f))
        assertEquals(21, navSqueezeSide(21f, 1f, 0f))
        assertEquals(0, navSqueezeSide(21f, 1f, 1f))
        assertEquals(11, navSqueezeSide(21f, 1f, 0.5f))
    }

    @Test
    fun `expanding pill keeps its rest size and fits inside the collapsed bar`() {
        for (requested in listOf(56, 64, 76)) {
            for (scale in listOf(0.8f, 1f, 1.2f)) {
                val geometry = navDockGeometry(true, true, requested.dp, 0.dp)
                val full = geometry.height.value
                val compact = geometry.compactHeight.value
                val pill = 44f * scale
                val rest = expandingPillHeight(pill, NavIndicator.Pill.pillSettle(full, full), full, 8f, 32f)
                assertEquals(minOf(pill, full - 8f).coerceAtLeast(32f), rest, 0.001f)
                val collapsed = expandingPillHeight(pill, NavIndicator.Pill.pillSettle(compact, full), compact, 8f, 32f)
                assertTrue("$requested at $scale", collapsed in 32f..(compact - 8f))
            }
        }
        assertEquals(33f, expandingPillHeight(44f, NavIndicator.Pill.pillSettle(48f, 64f), 48f, 8f, 32f), 0.001f)
        assertEquals(1f, NavIndicator.Floating.pillSettle(48f, 64f), 0f)
    }

    @Test
    fun `hidden and expanding labels reserve no space below icons`() {
        assertEquals(0.dp, navLabelRoom(NavLabelMode.Never, 48.dp))
        assertEquals(0.dp, navLabelRoom(NavLabelMode.Expanding, 48.dp))
    }
}
