package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScrollHeaderTest {
    @Test
    fun staysExpandedAtTheTop() {
        assertEquals(0f, homeHeaderCollapseProgress(0, 0, 88), 0.0001f)
    }

    @Test
    fun followsOffsetAndClamps() {
        assertEquals(0.5f, homeHeaderCollapseProgress(0, 44, 88), 0.0001f)
        assertEquals(1f, homeHeaderCollapseProgress(0, 200, 88), 0.0001f)
        assertEquals(0f, homeHeaderCollapseProgress(0, -10, 88), 0.0001f)
    }

    @Test
    fun collapsesWhenFirstItemLeavesViewport() {
        assertEquals(1f, homeHeaderCollapseProgress(1, 0, 88), 0.0001f)
    }

    @Test
    fun handlesInvalidDistance() {
        assertEquals(0f, homeHeaderCollapseProgress(0, 0, 0), 0.0001f)
        assertEquals(1f, homeHeaderCollapseProgress(0, 1, 0), 0.0001f)
    }
}
