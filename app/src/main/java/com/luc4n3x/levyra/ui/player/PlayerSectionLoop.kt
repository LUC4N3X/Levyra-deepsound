package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.player.SectionLoopState

internal fun formatSectionLoopTime(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

internal fun sectionLoopValue(state: SectionLoopState, setEndHint: String): String? = when (state) {
    SectionLoopState.Off -> null
    is SectionLoopState.Marking -> "${formatSectionLoopTime(state.startMs)} – $setEndHint"
    is SectionLoopState.Active -> "${formatSectionLoopTime(state.startMs)} – ${formatSectionLoopTime(state.endMs)}"
}
