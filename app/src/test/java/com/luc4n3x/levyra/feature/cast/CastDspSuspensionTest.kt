package com.luc4n3x.levyra.feature.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CastDspSuspensionTest {

    private val original = LocalDspSettings(
        equalizerEnabled = true,
        bassBoost = 65,
        virtualizer = 40,
        preampDb = 2.5f,
        limiterEnabled = true,
        normalizationEnabled = true,
        crossfadeSeconds = 6,
        autoMixEnabled = true
    )

    @Test
    fun suspendDisablesAllDspProcessingForRemotePlayback() {
        val snapshot = CastDspSuspension.suspendForRemote(original)

        assertEquals(false, snapshot.suspended.equalizerEnabled)
        assertEquals(0, snapshot.suspended.bassBoost)
        assertEquals(0, snapshot.suspended.virtualizer)
        assertEquals(0f, snapshot.suspended.preampDb)
        assertEquals(false, snapshot.suspended.limiterEnabled)
        assertEquals(false, snapshot.suspended.normalizationEnabled)
        assertEquals(0, snapshot.suspended.crossfadeSeconds)
        assertEquals(false, snapshot.suspended.autoMixEnabled)
    }

    @Test
    fun suspendNeverMutatesTheOriginalSettings() {
        val snapshot = CastDspSuspension.suspendForRemote(original)

        assertEquals(original, snapshot.original)
        assertNotEquals(snapshot.original, snapshot.suspended)
    }

    @Test
    fun restoreReturnsExactOriginalValuesAfterSuspension() {
        val snapshot = CastDspSuspension.suspendForRemote(original)

        val restored = CastDspSuspension.restoreLocal(snapshot)

        assertEquals(original, restored)
        assertEquals(original.equalizerEnabled, restored.equalizerEnabled)
        assertEquals(original.bassBoost, restored.bassBoost)
        assertEquals(original.virtualizer, restored.virtualizer)
        assertEquals(original.preampDb, restored.preampDb)
        assertEquals(original.limiterEnabled, restored.limiterEnabled)
        assertEquals(original.normalizationEnabled, restored.normalizationEnabled)
        assertEquals(original.crossfadeSeconds, restored.crossfadeSeconds)
        assertEquals(original.autoMixEnabled, restored.autoMixEnabled)
    }

    @Test
    fun restoreWorksForAlreadyFlatSettings() {
        val flat = LocalDspSettings(
            equalizerEnabled = false,
            bassBoost = 0,
            virtualizer = 0,
            preampDb = 0f,
            limiterEnabled = false,
            normalizationEnabled = false,
            crossfadeSeconds = 0,
            autoMixEnabled = false
        )

        val restored = CastDspSuspension.restoreLocal(CastDspSuspension.suspendForRemote(flat))

        assertEquals(flat, restored)
    }
}
