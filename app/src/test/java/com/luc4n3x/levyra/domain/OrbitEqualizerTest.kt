package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrbitEqualizerTest {
    private val frequencies = floatArrayOf(40f, 100f, 1_000f, 3_000f, 12_000f)

    private fun response(profile: ParametricEqProfile): FloatArray =
        FloatArray(frequencies.size).also { ParametricBiquad.responseDb(profile, frequencies, 48_000, it) }

    @Test
    fun `center is an exact flat profile with no attenuation`() {
        val shape = OrbitEqualizer.shape(OrbitPosition.Center, "Orbit")

        assertEquals(0f, shape.profile.preampDb, 0f)
        assertTrue(shape.profile.bands.all { it.gainDb == 0f })
        response(shape.profile).forEach { assertEquals(0f, it, 1e-4f) }
    }

    @Test
    fun `poles move the response in the expected direction`() {
        val warm = response(OrbitEqualizer.shape(OrbitPosition(-1f, 0f), "w").profile)
        val bright = response(OrbitEqualizer.shape(OrbitPosition(1f, 0f), "b").profile)
        val forward = response(OrbitEqualizer.shape(OrbitPosition(0f, 1f), "f").profile)
        val open = response(OrbitEqualizer.shape(OrbitPosition(0f, -1f), "o").profile)

        assertTrue(warm[1] > warm[4] + 10f)
        assertTrue(bright[4] > bright[1] + 10f)
        assertTrue(forward[2] > forward[0] && forward[2] > forward[4])
        assertTrue(open[2] < open[0] && open[2] < open[4])
    }

    @Test
    fun `headroom keeps every position at or below unity gain`() {
        var angle = 0.0
        while (angle < 2 * Math.PI) {
            for (radius in listOf(0.3f, 0.7f, 1f)) {
                val position = OrbitPosition((radius * kotlin.math.cos(angle)).toFloat(), (radius * kotlin.math.sin(angle)).toFloat())
                val shape = OrbitEqualizer.shape(position, "x")
                val dense = FloatArray(400) { (20.0 * Math.pow(1000.0, it / 399.0)).toFloat() }
                val out = FloatArray(dense.size)
                ParametricBiquad.responseDb(shape.profile, dense, 48_000, out)
                assertTrue("angle=$angle r=$radius max=${out.max()}", out.max() <= 0.05f)
                assertTrue(shape.loudnessOffsetDb <= 0f)
                assertNotNull(shape.profile.normalized())
            }
            angle += Math.PI / 12
        }
    }

    @Test
    fun `outside positions are projected onto the rim`() {
        val clamped = OrbitPosition(3f, 4f).clamped()

        assertEquals(1f, clamped.radius, 1e-5f)
        assertEquals(0.6f, clamped.tilt, 1e-5f)
        assertEquals(OrbitPosition.Center, OrbitPosition(Float.NaN, Float.POSITIVE_INFINITY).clamped())
    }

    @Test
    fun `position is read back from a stored orbit profile`() {
        val original = OrbitPosition(0.42f, -0.37f)
        val stored = OrbitEqualizer.shape(original, "Saved").profile.copy(id = "parametric_custom_x")

        assertEquals(original, OrbitEqualizer.positionOf(stored))
    }

    @Test
    fun `foreign profiles are not mistaken for orbit positions`() {
        val tweaked = OrbitEqualizer.shape(OrbitPosition(0.3f, 0.3f), "x").profile.let { profile ->
            profile.copy(bands = profile.bands.mapIndexed { index, band -> if (index == 2) band.copy(gainDb = band.gainDb + 2f) else band })
        }

        assertNull(OrbitEqualizer.positionOf(ParametricEqualizer.defaultProfile))
        assertNull(OrbitEqualizer.positionOf(tweaked))
        assertNull(OrbitEqualizer.positionOf(null))
    }

    @Test
    fun `comparison profile is level matched and never louder than unity`() {
        val shape = OrbitEqualizer.shape(OrbitPosition(-0.8f, -0.5f), "x")
        val compare = OrbitEqualizer.comparisonProfile(shape)

        assertTrue(compare.bands.all { it.gainDb == 0f })
        assertEquals(shape.loudnessOffsetDb, compare.preampDb, 1e-6f)
        assertTrue(compare.preampDb <= 0f)
        assertEquals(OrbitEqualizer.positionOf(compare), OrbitPosition.Center)
    }
}
