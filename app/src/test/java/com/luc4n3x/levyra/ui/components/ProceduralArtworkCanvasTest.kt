package com.luc4n3x.levyra.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProceduralArtworkCanvasTest {

    @Test
    fun handlesMinIntBoundaryWithoutException() {
        // "polygenelubricants" is a known Java/Kotlin String with hashCode() == Int.MIN_VALUE (-2147483648)
        val minIntSeed = "polygenelubricants"
        assertEquals(Int.MIN_VALUE, minIntSeed.hashCode())

        val darkParams = generateProceduralParams(minIntSeed, darkTheme = true)
        val lightParams = generateProceduralParams(minIntSeed, darkTheme = false)

        assertNotNull(darkParams)
        assertNotNull(lightParams)

        assertEquals(3, darkParams.blobs.size)
        assertEquals(3, darkParams.rings.size)
        assertEquals(3, lightParams.blobs.size)
        assertEquals(3, lightParams.rings.size)

        assertTrue(darkParams.centerXRatio in 0f..1f)
        assertTrue(darkParams.centerYRatio in 0f..1f)
        assertTrue(lightParams.centerXRatio in 0f..1f)
        assertTrue(lightParams.centerYRatio in 0f..1f)
    }

    @Test
    fun generateProceduralParamsExecutesForVariousSeeds() {
        val seeds = listOf(
            "Radiohead - Karma Police",
            "Daft Punk - One More Time",
            "",
            "A",
            "1234567890"
        )

        for (seed in seeds) {
            val darkParams = generateProceduralParams(seed, darkTheme = true)
            val lightParams = generateProceduralParams(seed, darkTheme = false)

            assertNotNull(darkParams.backgroundBrush)
            assertNotNull(lightParams.backgroundBrush)

            assertEquals(3, darkParams.blobs.size)
            assertEquals(3, darkParams.rings.size)

            darkParams.blobs.forEach { blob ->
                assertTrue(blob.sizeRatio > 0f)
                assertTrue(blob.alpha in 0f..1f)
            }
            darkParams.rings.forEach { ring ->
                assertTrue(ring.radiusRatio > 0f)
                assertTrue(ring.strokeWidth > 0f)
                assertTrue(ring.alpha in 0f..1f)
            }
        }
    }
}
