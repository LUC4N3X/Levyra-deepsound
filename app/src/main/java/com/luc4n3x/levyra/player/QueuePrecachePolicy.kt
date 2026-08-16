package com.luc4n3x.levyra.player

internal fun isQueuePrecacheFresh(
    preparedAtElapsedMs: Long,
    nowElapsedMs: Long,
    maxAgeMs: Long
): Boolean {
    if (preparedAtElapsedMs < 0L || nowElapsedMs < preparedAtElapsedMs || maxAgeMs < 0L) return false
    return nowElapsedMs - preparedAtElapsedMs <= maxAgeMs
}

internal fun queuePrecacheMatchesTransition(
    preparedSourceIdentity: String,
    preparedTargetIdentity: String,
    currentSourceIdentity: String,
    currentTargetIdentity: String,
    preparedAtElapsedMs: Long,
    nowElapsedMs: Long,
    maxAgeMs: Long
): Boolean =
    preparedSourceIdentity == currentSourceIdentity &&
        preparedTargetIdentity == currentTargetIdentity &&
        isQueuePrecacheFresh(preparedAtElapsedMs, nowElapsedMs, maxAgeMs)

internal fun queuePrecacheMatchesTarget(
    preparedTargetIdentity: String,
    requestedTargetIdentity: String,
    preparedAtElapsedMs: Long,
    nowElapsedMs: Long,
    maxAgeMs: Long
): Boolean =
    preparedTargetIdentity == requestedTargetIdentity &&
        isQueuePrecacheFresh(preparedAtElapsedMs, nowElapsedMs, maxAgeMs)

internal fun queuePrecacheMatchesTrackId(
    targetTrackId: String,
    resolvedTrackId: String,
    requestedTrackId: String
): Boolean =
    requestedTrackId.isNotBlank() &&
        (requestedTrackId == targetTrackId || requestedTrackId == resolvedTrackId)
