package com.luc4n3x.levyra.nexus.playback

enum class LevyraTransitionReason {
    USER_NEXT,
    USER_PREVIOUS,
    USER_SELECT,
    AUTO_ADVANCE,
    REPEAT,
    ERROR_RECOVERY,
    QUEUE_REPLACED
}

data class LevyraPlaybackSnapshot(
    val trackId: String,
    val positionMs: Long,
    val durationMs: Long,
    val ended: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val completionRatio: Double
        get() = if (durationMs <= 0L) 0.0 else positionMs.coerceIn(0L, durationMs).toDouble() / durationMs.toDouble()
}

sealed interface LevyraPlaybackSignal {
    val trackId: String

    data class Completed(
        override val trackId: String,
        val completionRatio: Double,
        val reason: LevyraTransitionReason
    ) : LevyraPlaybackSignal

    data class Skipped(
        override val trackId: String,
        val completionRatio: Double,
        val reason: LevyraTransitionReason
    ) : LevyraPlaybackSignal

    data class Replayed(
        override val trackId: String
    ) : LevyraPlaybackSignal

    data class Failed(
        override val trackId: String,
        val completionRatio: Double
    ) : LevyraPlaybackSignal

    data class Transitioned(
        override val trackId: String,
        val completionRatio: Double,
        val reason: LevyraTransitionReason
    ) : LevyraPlaybackSignal
}

class LevyraPlaybackCoordinator(
    private val completionThreshold: Double = 0.82
) {
    init {
        require(completionThreshold in 0.5..1.0)
    }

    fun classify(
        snapshot: LevyraPlaybackSnapshot,
        reason: LevyraTransitionReason
    ): LevyraPlaybackSignal {
        val ratio = snapshot.completionRatio
        return when (reason) {
            LevyraTransitionReason.AUTO_ADVANCE -> if (snapshot.ended || ratio >= completionThreshold) {
                LevyraPlaybackSignal.Completed(
                    trackId = snapshot.trackId,
                    completionRatio = if (snapshot.ended) 1.0 else ratio,
                    reason = reason
                )
            } else {
                LevyraPlaybackSignal.Transitioned(snapshot.trackId, ratio, reason)
            }
            LevyraTransitionReason.REPEAT -> LevyraPlaybackSignal.Replayed(snapshot.trackId)
            LevyraTransitionReason.ERROR_RECOVERY -> LevyraPlaybackSignal.Failed(snapshot.trackId, ratio)
            LevyraTransitionReason.QUEUE_REPLACED -> LevyraPlaybackSignal.Transitioned(snapshot.trackId, ratio, reason)
            LevyraTransitionReason.USER_NEXT,
            LevyraTransitionReason.USER_PREVIOUS,
            LevyraTransitionReason.USER_SELECT -> if (snapshot.ended || ratio >= completionThreshold) {
                LevyraPlaybackSignal.Completed(snapshot.trackId, if (snapshot.ended) 1.0 else ratio, reason)
            } else {
                LevyraPlaybackSignal.Skipped(snapshot.trackId, ratio, reason)
            }
        }
    }
}
