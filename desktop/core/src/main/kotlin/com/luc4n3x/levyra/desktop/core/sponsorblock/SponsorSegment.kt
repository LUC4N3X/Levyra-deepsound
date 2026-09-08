package com.luc4n3x.levyra.desktop.core.sponsorblock

const val SPONSOR_SEGMENT_ACTION_SKIP = "skip"

data class SponsorSegment(
    val startMs: Long,
    val endMs: Long,
    val category: String,
    val uuid: String = "",
    val actionType: String = SPONSOR_SEGMENT_ACTION_SKIP
)

data class SponsorSkipDecision(
    val targetPositionMs: Long,
    val skippedIdentities: List<String>
)

object SponsorSkipPlanner {

    const val SKIP_GUARD_MS = 250L

    fun identityOf(segment: SponsorSegment): String =
        segment.uuid.takeIf { it.isNotBlank() }
            ?: "${segment.startMs}:${segment.endMs}:${segment.category}"

    fun decide(
        segments: List<SponsorSegment>,
        positionMs: Long,
        alreadySkipped: Set<String>
    ): SponsorSkipDecision? {
        val covering = segments.filter { segment -> segment.covers(positionMs) }
        if (covering.isEmpty()) return null
        val target = covering
            .filterNot { segment -> identityOf(segment) in alreadySkipped }
            .maxOfOrNull { segment -> segment.endMs }
            ?: return null
        return SponsorSkipDecision(
            targetPositionMs = target,
            skippedIdentities = covering.map(::identityOf)
        )
    }

    private fun SponsorSegment.covers(positionMs: Long): Boolean =
        actionType.equals(SPONSOR_SEGMENT_ACTION_SKIP, ignoreCase = true) &&
            endMs > startMs &&
            positionMs >= startMs &&
            positionMs < endMs - SKIP_GUARD_MS
}

class SponsorSkipTracker {

    private val lock = Any()
    private var boundMediaKey: String = ""
    private var boundSegments: List<SponsorSegment> = emptyList()
    private val skippedIdentities = LinkedHashSet<String>()

    fun bind(mediaKey: String) {
        synchronized(lock) {
            boundMediaKey = mediaKey
            boundSegments = emptyList()
            skippedIdentities.clear()
        }
    }

    fun reset() {
        bind("")
    }

    fun planSkip(mediaKey: String, positionMs: Long, segments: List<SponsorSegment>): Long? =
        synchronized(lock) {
            if (mediaKey.isBlank() || boundMediaKey != mediaKey) return@synchronized null
            if (boundSegments !== segments) {
                boundSegments = segments
                skippedIdentities.clear()
            }
            val decision = SponsorSkipPlanner.decide(segments, positionMs, skippedIdentities)
                ?: return@synchronized null
            skippedIdentities += decision.skippedIdentities
            decision.targetPositionMs
        }
}
