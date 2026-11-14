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

    private val tiltWeights = floatArrayOf(-1f, -0.45f, 0f, 0.55f, 1f)
    private val contourWeights = floatArrayOf(-0.45f, -0.1f, 0.85f, 0.55f, -0.4f)

    private fun legacyGains(position: OrbitPosition): FloatArray {
        val clamped = position.clamped()
        return FloatArray(tiltWeights.size) { band ->
            OrbitEqualizer.DEPTH_DB * (clamped.tilt * tiltWeights[band] + clamped.contour * contourWeights[band])
        }
    }

    @Test
    fun `default pole angles reproduce the legacy two axis orbit exactly`() {
        for (tiltStep in -12..12) {
            for (contourStep in -12..12) {
                val position = OrbitPosition(tiltStep / 10f, contourStep / 10f)
                val expected = legacyGains(position)
                val gains = OrbitEqualizer.bandGains(position)
                gains.indices.forEach { band -> assertEquals("p=$position band=$band", expected[band], gains[band], 0f) }
            }
        }
    }

    @Test
    fun `profiles saved with the legacy math restore unchanged`() {
        val position = OrbitPosition(-0.55f, 0.35f)
        val template = OrbitEqualizer.shape(OrbitPosition.Center, "Orbit").profile
        val gains = legacyGains(position)
        val stored = template.copy(id = "parametric_custom_old", preampDb = -6.4f, bands = template.bands.mapIndexed { index, band -> band.copy(gainDb = gains[index]) })
        val stale = OrbitLayout(listOf(40f, 45f, 50f, 270f), OrbitPosition(0.7f, 0.7f))

        assertEquals(OrbitLayout(puck = position), OrbitEqualizer.restore(stored, null))
        assertEquals(OrbitLayout(puck = position), OrbitEqualizer.restore(stored, stale))
        val replayed = OrbitEqualizer.shape(position, "Orbit").profile
        stored.bands.indices.forEach { assertEquals(stored.bands[it].gainDb, replayed.bands[it].gainDb, 0f) }
    }

    @Test
    fun `grouped warm bright and forward poles add up across the spectrum`() {
        val angles = listOf(40f, 45f, 50f, 270f)
        val puck = OrbitPosition(0.7f, 0.7f)
        val weights = OrbitEqualizer.poleWeights(puck, angles)
        val gains = OrbitEqualizer.bandGains(puck, angles)

        (0..2).forEach { assertTrue("pole $it", weights[it] > 0.9f) }
        assertEquals(0f, weights[3], 0f)
        assertTrue("bass ${gains[0]}", gains[0] > 5f)
        assertTrue("mids ${gains[2]}", gains[2] > 5f)
        assertTrue("treble ${gains[4]}", gains[4] > 5f)
        val vectors = listOf(tiltWeights.map { maxOf(0f, -it) }, tiltWeights.map { maxOf(0f, it) }, contourWeights.map { maxOf(0f, it) }, contourWeights.map { maxOf(0f, -it) })
        gains.indices.forEach { band ->
            val expected = OrbitEqualizer.DEPTH_DB * weights.indices.sumOf { (weights[it] * vectors[it][band]).toDouble() }.toFloat()
            assertEquals(expected, gains[band], 1e-4f)
        }
        val shape = OrbitEqualizer.shape(puck, "x", angles)
        assertNotNull(shape.profile.normalized())
        assertTrue(shape.profile.preampDb < 0f)
    }

    @Test
    fun `centre is flat for any pole layout`() {
        listOf(OrbitEqualizer.DEFAULT_POLE_ANGLES, listOf(40f, 45f, 50f, 270f), listOf(0f, 0f, 0f, 0f)).forEach { angles ->
            assertTrue(OrbitEqualizer.bandGains(OrbitPosition.Center, angles).all { it == 0f })
        }
    }

    @Test
    fun `custom layout is restored from its own profile and legacy profiles still restore`() {
        val layout = OrbitLayout(listOf(120f, 30f, 60f, 270f), OrbitPosition(0.5f, 0.6f))
        val profile = OrbitEqualizer.shape(layout.puck, "Orbit", layout.poleAngles).profile

        assertEquals(layout, OrbitEqualizer.restore(profile, layout))
        val legacy = OrbitEqualizer.shape(OrbitPosition(0.42f, -0.37f), "Orbit").profile
        assertEquals(OrbitLayout(puck = OrbitPosition(0.42f, -0.37f)), OrbitEqualizer.restore(legacy, null))
        assertEquals(OrbitLayout(puck = OrbitPosition(0.42f, -0.37f)), OrbitEqualizer.restore(legacy, layout))
        assertNull(OrbitEqualizer.restore(ParametricEqualizer.defaultProfile, layout))
    }

    @Test
    fun `layout survives an encode decode round trip and rejects garbage`() {
        val layout = OrbitLayout(listOf(181.5f, 0f, 92.25f, 359f), OrbitPosition(-0.31f, 0.44f))

        assertEquals(layout, OrbitLayout.decode(layout.encoded()))
        assertEquals(listOf(180f, 0f, 90f, 270f), OrbitLayout.decode("540,-360,450,-90;0,0")?.poleAngles)
        assertNull(OrbitLayout.decode(null))
        assertNull(OrbitLayout.decode(""))
        assertNull(OrbitLayout.decode("1,2,3;0,0"))
        assertNull(OrbitLayout.decode("1,2,3,NaN;0,0"))
        assertNull(OrbitLayout.decode("1,2,3,4;x,0"))
    }
}
