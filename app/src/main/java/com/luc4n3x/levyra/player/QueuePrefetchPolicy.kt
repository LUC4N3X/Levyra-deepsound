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
