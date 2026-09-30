package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassBackdropPolicyTest {
    @Test
    fun `quality buckets follow the glass blur families`() {
        assertEquals(GlassBackdropQuality.Native, resolveGlassBackdropQuality(18f))
        assertEquals(GlassBackdropQuality.Balanced, resolveGlassBackdropQuality(24f))
        assertEquals(GlassBackdropQuality.Efficient, resolveGlassBackdropQuality(30f))
    }

    @Test
    fun `scaled layer rounds outward and never becomes empty`() {
        assertEquals(76, glassBackdropLayerDimension(101f, 0.75f))
        assertEquals(1, glassBackdropLayerDimension(0f, 0.5f))
    }
}
