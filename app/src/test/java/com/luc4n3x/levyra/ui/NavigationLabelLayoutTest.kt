package com.luc4n3x.levyra.ui

import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.NavLabelMode
import com.luc4n3x.levyra.ui.theme.levyraLineHeightSp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationLabelLayoutTest {
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
    fun `hidden and expanding labels reserve no space below icons`() {
        assertEquals(0.dp, navLabelRoom(NavLabelMode.Never, 48.dp))
        assertEquals(0.dp, navLabelRoom(NavLabelMode.Expanding, 48.dp))
    }
}
