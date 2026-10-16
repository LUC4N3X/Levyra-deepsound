package com.luc4n3x.levyra.player

internal const val QUEUE_PREFETCH_MAX_DEPTH = 3

private const val QUEUE_PREFETCH_NEAR_DIVISOR = 2L
private const val QUEUE_PREFETCH_NEAR_MIN_BYTES = 128L * 1024L
private const val QUEUE_PREFETCH_FAR_BYTES = 64L * 1024L

internal fun queuePrefetchPrimeDepth(
    resolveCount: Int,
    videoMode: Boolean,
    lowRam: Boolean,
    powerConstrained: Boolean,
    unmetered: Boolean
): Int {
    if (resolveCount <= 0) return 0
    val depth = when {
        videoMode -> 1
        lowRam || powerConstrained -> 1
        unmetered -> QUEUE_PREFETCH_MAX_DEPTH
        else -> 2
    }
    return depth.coerceIn(1, minOf(resolveCount, QUEUE_PREFETCH_MAX_DEPTH))
}

internal fun queuePrefetchPrimeBytes(distanceIndex: Int, basePrimeBytes: Long): Long {
    if (distanceIndex < 0 || basePrimeBytes <= 0L) return 0L
    return when (distanceIndex) {
        0 -> basePrimeBytes
        1 -> (basePrimeBytes / QUEUE_PREFETCH_NEAR_DIVISOR)
            .coerceAtLeast(QUEUE_PREFETCH_NEAR_MIN_BYTES)
            .coerceAtMost(basePrimeBytes)
        else -> QUEUE_PREFETCH_FAR_BYTES.coerceAtMost(basePrimeBytes)
    }
}

internal const val ADVANCED_PREFETCH_MAX_TRACK_BYTES = 48L * 1024L * 1024L
internal const val ADVANCED_PREFETCH_MIN_TRACK_BYTES = 1024L * 1024L
internal const val ADVANCED_PREFETCH_MIN_FREE_DISK_BYTES = 256L * 1024L * 1024L
internal const val ADVANCED_PREFETCH_START_DELAY_MS = 8_000L
internal const val ADVANCED_PREFETCH_TRACK_GAP_MS = 1_500L

internal data class AdvancedPrefetchConditions(
    val windowSize: Int,
    val videoMode: Boolean,
    val connected: Boolean,
    val unmetered: Boolean,
    val mobileDataAllowed: Boolean = false,
    val lowRam: Boolean,
    val powerConstrained: Boolean,
    val freeDiskBytes: Long
)

internal fun advancedPrefetchAllowed(conditions: AdvancedPrefetchConditions): Boolean =
    conditions.windowSize > 0 &&
        !conditions.videoMode &&
        conditions.connected &&
        (conditions.unmetered || conditions.mobileDataAllowed) &&
        !conditions.lowRam &&
        !conditions.powerConstrained &&
        conditions.freeDiskBytes >= ADVANCED_PREFETCH_MIN_FREE_DISK_BYTES

internal fun advancedPrefetchNetworkAllowed(
    internet: Boolean,
    validated: Boolean,
    unmetered: Boolean,
    mobileDataAllowed: Boolean,
    dataSaverOn: Boolean
): Boolean = internet && validated && (unmetered || (mobileDataAllowed && !dataSaverOn))

internal const val RADIO_TAIL_MIN_UPCOMING = 3
private const val RADIO_TAIL_FETCH_HEADROOM = 5
private const val RADIO_TAIL_MAX_FETCH = 30

internal fun radioTailTargetUpcoming(advancedPrefetchTracks: Int): Int =
    maxOf(RADIO_TAIL_MIN_UPCOMING, advancedPrefetchTracks)

internal fun radioTailFetchLimit(targetUpcoming: Int, currentUpcoming: Int): Int =
    (targetUpcoming - currentUpcoming.coerceAtLeast(0) + RADIO_TAIL_FETCH_HEADROOM)
        .coerceIn(RADIO_TAIL_FETCH_HEADROOM, RADIO_TAIL_MAX_FETCH)

internal fun advancedPrefetchBudgetBytes(cacheMaxBytes: Long): Long =
    (cacheMaxBytes / 2L).coerceAtLeast(0L)

internal fun advancedPrefetchTrackLimit(usedBytes: Long, budgetBytes: Long): Long {
    val remaining = budgetBytes - usedBytes.coerceAtLeast(0L)
    if (remaining < ADVANCED_PREFETCH_MIN_TRACK_BYTES) return 0L
    return remaining.coerceAtMost(ADVANCED_PREFETCH_MAX_TRACK_BYTES)
}

internal fun isProgressiveMediaMimeType(mimeType: String?): Boolean {
    val normalized = mimeType.orEmpty().substringBefore(';').trim().lowercase()
    return normalized.startsWith("audio/") || normalized.startsWith("video/")
}
