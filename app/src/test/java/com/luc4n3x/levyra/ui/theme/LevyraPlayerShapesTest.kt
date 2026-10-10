package com.luc4n3x.levyra.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraPlayerShapesTest {

    @Test
    fun negativeRuntimeCornerIsClampedToZero() {
        assertEquals(0.dp, nonNegativeCornerRadius((-1.25f).dp))
    }

    @Test
    fun validRuntimeCornerIsPreserved() {
        assertEquals(18.dp, nonNegativeCornerRadius(18.dp))
    }
}
