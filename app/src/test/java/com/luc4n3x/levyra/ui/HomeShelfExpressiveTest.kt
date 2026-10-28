package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeShelfExpressiveTest {

    @Test
    fun `header collapse starts at rest and completes before the header leaves`() {
        assertEquals(0f, homeHeaderCollapse(0f, 200f), 0.0001f)
        assertTrue(homeHeaderCollapse(50f, 200f) in 0.01f..0.99f)
        assertEquals(1f, homeHeaderCollapse(110f, 200f), 0.0001f)
        assertEquals(1f, homeHeaderCollapse(10_000f, 200f), 0.0001f)
    }

    @Test
    fun `unmeasured header never collapses`() {
        assertEquals(0f, homeHeaderCollapse(80f, 0f), 0.0001f)
        assertEquals(0f, homeHeaderCollapse(-20f, 200f), 0.0001f)
    }
}
