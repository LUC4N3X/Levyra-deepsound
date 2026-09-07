package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.SponsorSegment

internal const val SPONSOR_BLOCK_SKIP_GUARD_MS = 250L

internal fun sponsorSegmentIdentity(segment: SponsorSegment): String =
    segment.uuid.takeIf { it.isNotBlank() }
        ?: "${segment.startMs}:${segment.endMs}:${segment.category}"

internal data class SponsorBlockSkipDecision(
    val targetPositionMs: Long,
    val encounteredIdentities: List<String>
)

private fun SponsorSegment.coversSkipPosition(positionMs: Long): Boolean =
    endMs > startMs &&
        positionMs >= startMs &&
        positionMs < endMs - SPONSOR_BLOCK_SKIP_GUARD_MS

internal fun sponsorBlockSkipDecision(
    segments: List<SponsorSegment>,
    positionMs: Long,
    consumedIdentities: Set<String>
): SponsorBlockSkipDecision? {
    var targetPositionMs = Long.MIN_VALUE
    for (segment in segments) {
        if (!segment.coversSkipPosition(positionMs)) continue
        if (sponsorSegmentIdentity(segment) in consumedIdentities) continue
        if (segment.endMs > targetPositionMs) targetPositionMs = segment.endMs
    }
    if (targetPositionMs == Long.MIN_VALUE) return null
    val encountered = ArrayList<String>(2)
    for (segment in segments) {
        if (segment.coversSkipPosition(positionMs)) encountered += sponsorSegmentIdentity(segment)
    }
    return SponsorBlockSkipDecision(targetPositionMs, encountered)
}

/**
 * Skip-once bookkeeping for one media item. The consumed set is bounded by the segment count of the
 * bound media and is cleared whenever a different media item or a new playback session takes over,
 * so consumed state can never leak across videos.
 */
internal class SponsorBlockSkipOnceTracker {

    private val lock = Any()
    private var boundMediaKey: String? = null
    private val consumedIdentities = LinkedHashSet<String>()

    val activeMediaKey: String?
        get() = synchronized(lock) { boundMediaKey }

    val consumedCount: Int
        get() = synchronized(lock) { consumedIdentities.size }

    fun bind(mediaKey: String) {
        synchronized(lock) {
            if (boundMediaKey == mediaKey) return@synchronized
            boundMediaKey = mediaKey
            consumedIdentities.clear()
        }
    }

    fun beginPlayback(mediaKey: String) {
        synchronized(lock) {
            boundMediaKey = mediaKey
            consumedIdentities.clear()
        }
    }

    fun reset() {
        synchronized(lock) {
            boundMediaKey = null
            consumedIdentities.clear()
        }
    }

    fun planSkip(mediaKey: String, positionMs: Long, segments: List<SponsorSegment>): Long? =
        synchronized(lock) {
            if (boundMediaKey != mediaKey) return@synchronized null
            val decision = sponsorBlockSkipDecision(segments, positionMs, consumedIdentities)
                ?: return@synchronized null
            consumedIdentities += decision.encounteredIdentities
            decision.targetPositionMs
        }
}
