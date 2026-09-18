package com.luc4n3x.levyra.nativeaudio

import androidx.media3.common.C

object OboeOutputPolicy {
    const val MIN_SDK = 28
    private const val MIN_SAMPLE_RATE = 8_000
    private const val MAX_SAMPLE_RATE = 384_000
    private const val BYTES_PER_PCM16_SAMPLE = 2

    fun isEligible(
        encoding: Int,
        channelCount: Int,
        sampleRate: Int,
        isOffload: Boolean,
        isTunneling: Boolean,
        usesPlatformPlaybackParameters: Boolean
    ): Boolean =
        encoding == C.ENCODING_PCM_16BIT &&
            channelCount in 1..2 &&
            sampleRate in MIN_SAMPLE_RATE..MAX_SAMPLE_RATE &&
            !isOffload &&
            !isTunneling &&
            !usesPlatformPlaybackParameters

    fun channelCountOf(channelMask: Int): Int = Integer.bitCount(channelMask)

    fun ringCapacityFrames(bufferSizeBytes: Int, channelCount: Int, sampleRate: Int): Int {
        val frameBytes = channelCount * BYTES_PER_PCM16_SAMPLE
        val requested = if (frameBytes > 0) bufferSizeBytes / frameBytes else 0
        return requested.coerceIn(sampleRate / 10, sampleRate * 2)
    }
}

class OboeFailureGuard(
    private val maxFailures: Int = 3,
    private val windowMs: Long = 60_000L
) {
    private val failureTimes = LongArray(maxFailures)
    private var failureCount = 0
    private var nextSlot = 0

    @Volatile
    var isTripped: Boolean = false
        private set

    @Synchronized
    fun recordFailure(nowMs: Long) {
        if (isTripped) return
        failureTimes[nextSlot] = nowMs
        nextSlot = (nextSlot + 1) % maxFailures
        if (failureCount < maxFailures) failureCount++
        if (failureCount == maxFailures && nowMs - failureTimes[nextSlot] <= windowMs) {
            isTripped = true
        }
    }
}
