package com.luc4n3x.levyra.nativeaudio

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OboeOutputPolicyTest {
    @Test
    fun acceptsPcm16StereoAndMonoOnly() {
        assertTrue(eligible(channelCount = 2))
        assertTrue(eligible(channelCount = 1))
        assertFalse(eligible(channelCount = 6))
        assertFalse(eligible(encoding = C.ENCODING_PCM_FLOAT))
        assertFalse(eligible(encoding = C.ENCODING_AC3))
    }

    @Test
    fun rejectsOffloadTunnelingAndPlatformSpeed() {
        assertFalse(eligible(isOffload = true))
        assertFalse(eligible(isTunneling = true))
        assertFalse(eligible(usesPlatformPlaybackParameters = true))
    }

    @Test
    fun rejectsImplausibleSampleRates() {
        assertFalse(eligible(sampleRate = 4_000))
        assertFalse(eligible(sampleRate = 768_000))
        assertTrue(eligible(sampleRate = 192_000))
    }

    @Test
    fun channelCountFollowsOutputMaskBits() {
        assertEquals(1, OboeOutputPolicy.channelCountOf(0x4))
        assertEquals(2, OboeOutputPolicy.channelCountOf(0xC))
        assertEquals(6, OboeOutputPolicy.channelCountOf(0xFC))
    }

    @Test
    fun ringCapacityIsBoundedBetweenHundredMsAndTwoSeconds() {
        assertEquals(24_000, OboeOutputPolicy.ringCapacityFrames(96_000, 2, 48_000))
        assertEquals(4_800, OboeOutputPolicy.ringCapacityFrames(100, 2, 48_000))
        assertEquals(96_000, OboeOutputPolicy.ringCapacityFrames(Int.MAX_VALUE, 2, 48_000))
        assertEquals(4_800, OboeOutputPolicy.ringCapacityFrames(96_000, 0, 48_000))
    }

    @Test
    fun failureGuardTripsOnlyForBurstsInsideTheWindow() {
        val guard = OboeFailureGuard(maxFailures = 3, windowMs = 60_000L)
        guard.recordFailure(0L)
        guard.recordFailure(70_000L)
        guard.recordFailure(140_000L)
        assertFalse(guard.isTripped)
        guard.recordFailure(150_000L)
        assertFalse(guard.isTripped)
        guard.recordFailure(160_000L)
        assertTrue(guard.isTripped)
    }

    @Test
    fun failureGuardStaysTrippedForItsLifetime() {
        val guard = OboeFailureGuard(maxFailures = 2, windowMs = 1_000L)
        guard.recordFailure(0L)
        guard.recordFailure(10L)
        assertTrue(guard.isTripped)
        guard.recordFailure(1_000_000L)
        assertTrue(guard.isTripped)
    }

    private fun eligible(
        encoding: Int = C.ENCODING_PCM_16BIT,
        channelCount: Int = 2,
        sampleRate: Int = 48_000,
        isOffload: Boolean = false,
        isTunneling: Boolean = false,
        usesPlatformPlaybackParameters: Boolean = false
    ): Boolean = OboeOutputPolicy.isEligible(
        encoding,
        channelCount,
        sampleRate,
        isOffload,
        isTunneling,
        usesPlatformPlaybackParameters
    )
}
