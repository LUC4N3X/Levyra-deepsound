package com.luc4n3x.levyra.player

internal object PlaybackTrackSelectionPolicy {
    fun disableVideoTracks(videoMode: Boolean): Boolean = !videoMode
}

internal object PlaybackMemoryGuardPolicy {
    const val SAMPLE_INTERVAL_MS = 15_000L
    const val REQUIRED_HIGH_SAMPLES = 3
    const val COOLDOWN_MS = 120_000L

    private const val MIN_THRESHOLD_BYTES = 512L * 1024L * 1024L
    private const val MAX_THRESHOLD_BYTES = 2048L * 1024L * 1024L
    private const val STANDARD_FRACTION = 0.18
    private const val LOW_RAM_FRACTION = 0.10

    fun thresholdBytes(totalDeviceMemoryBytes: Long, lowRamDevice: Boolean): Long {
        if (totalDeviceMemoryBytes <= 0L) return MIN_THRESHOLD_BYTES
        val fraction = if (lowRamDevice) LOW_RAM_FRACTION else STANDARD_FRACTION
        val scaled = (totalDeviceMemoryBytes.toDouble() * fraction).toLong()
        return scaled.coerceIn(MIN_THRESHOLD_BYTES, MAX_THRESHOLD_BYTES)
    }

    fun nextHighSampleCount(current: Int, nativeAllocatedBytes: Long, thresholdBytes: Long): Int {
        return if (nativeAllocatedBytes >= thresholdBytes) current + 1 else 0
    }

    fun shouldRecycle(
        highSamples: Int,
        nowElapsedMs: Long,
        lastRecycleElapsedMs: Long,
        requiredSamples: Int = REQUIRED_HIGH_SAMPLES,
        cooldownMs: Long = COOLDOWN_MS
    ): Boolean {
        if (highSamples < requiredSamples) return false
        if (lastRecycleElapsedMs <= 0L) return true
        return nowElapsedMs - lastRecycleElapsedMs >= cooldownMs
    }
}
