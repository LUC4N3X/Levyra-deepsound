package com.luc4n3x.levyra.player

const val SECTION_LOOP_MIN_LENGTH_MS = 1_000L
const val SECTION_LOOP_POLL_MS = 150L
const val SECTION_LOOP_MIN_POLL_MS = 20L

sealed interface SectionLoopState {
    data object Off : SectionLoopState
    data class Marking(val mediaId: String, val startMs: Long) : SectionLoopState
    data class Active(val mediaId: String, val startMs: Long, val endMs: Long) : SectionLoopState
}

fun SectionLoopState.advance(mediaId: String, positionMs: Long): SectionLoopState {
    val position = positionMs.coerceAtLeast(0L)
    return when (this) {
        SectionLoopState.Off -> SectionLoopState.Marking(mediaId, position)
        is SectionLoopState.Marking -> when {
            this.mediaId != mediaId -> SectionLoopState.Marking(mediaId, position)
            position - startMs >= SECTION_LOOP_MIN_LENGTH_MS -> SectionLoopState.Active(mediaId, startMs, position)
            startMs - position >= SECTION_LOOP_MIN_LENGTH_MS -> SectionLoopState.Active(mediaId, position, startMs)
            else -> this
        }
        is SectionLoopState.Active -> SectionLoopState.Off
    }
}

fun SectionLoopState.belongsTo(mediaId: String?): Boolean = when (this) {
    SectionLoopState.Off -> true
    is SectionLoopState.Marking -> this.mediaId == mediaId
    is SectionLoopState.Active -> this.mediaId == mediaId
}

fun sectionLoopSeekTarget(state: SectionLoopState, mediaId: String?, positionMs: Long): Long? {
    if (state !is SectionLoopState.Active || state.mediaId != mediaId) return null
    return if (positionMs >= state.endMs || positionMs < state.startMs - SECTION_LOOP_MIN_LENGTH_MS) state.startMs else null
}

fun sectionLoopRestartTarget(state: SectionLoopState, mediaId: String?): Long? =
    (state as? SectionLoopState.Active)?.takeIf { it.mediaId == mediaId }?.startMs

fun sectionLoopPollDelayMs(state: SectionLoopState, positionMs: Long, playbackSpeed: Float): Long {
    if (state !is SectionLoopState.Active) return SECTION_LOOP_POLL_MS
    val remainingWallClockMs = (state.endMs - positionMs).coerceAtLeast(0L) / playbackSpeed.coerceAtLeast(0.1f)
    return remainingWallClockMs.toLong().coerceIn(SECTION_LOOP_MIN_POLL_MS, SECTION_LOOP_POLL_MS)
}
