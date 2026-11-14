package com.luc4n3x.levyra.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrbitPoleDragTest {
    @Test
    fun `pole drag is relative so a stacked label does not jump`() {
        val ring = Offset(141f, 141f)
        val stacked = Offset(141f, 93f)

        listOf(ring, stacked).forEach { start ->
            assertEquals(45f, draggedPoleAngle(45f, start, start), 1e-3f)
            val moved = draggedPoleAngle(45f, start, start + Offset(-5f, 0f))
            assertTrue("start=$start moved=$moved", moved > 45f && moved < 48f)
        }
    }

    @Test
    fun `pole drag wraps across the seam`() {
        val start = Offset(-200f, 1f)
        val finger = Offset(-200f, -1f)

        assertEquals(181f, draggedPoleAngle(180f, start, finger), 0.5f)
        assertEquals(1f, draggedPoleAngle(0f, start, finger), 0.5f)
    }
}
