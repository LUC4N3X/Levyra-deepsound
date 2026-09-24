package com.luc4n3x.levyra.ui.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luc4n3x.levyra.domain.Track
import java.util.Locale

@Stable
internal class TrackSelectionState {
    var selectedIds: Set<String> by mutableStateOf(emptySet())
        private set

    val isActive: Boolean
        get() = selectedIds.isNotEmpty()

    val count: Int
        get() = selectedIds.size

    fun isSelected(id: String): Boolean = id.isNotBlank() && id in selectedIds

    fun start(id: String) {
        val clean = id.trim()
        if (clean.isBlank()) return
        if (isActive) {
            toggle(clean)
        } else {
            selectedIds = linkedSetOf(clean)
        }
    }

    fun toggle(id: String) {
        val clean = id.trim()
        if (clean.isBlank()) return
        selectedIds = if (clean in selectedIds) {
            selectedIds - clean
        } else {
            LinkedHashSet<String>(selectedIds.size + 1).apply {
                addAll(selectedIds)
                add(clean)
            }
        }
    }

    fun selectAll(ids: Iterable<String>) {
        val available = ids.asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .toCollection(LinkedHashSet())
        selectedIds = available
    }

    fun deselectAll() {
        selectedIds = emptySet()
    }

    fun toggleSelectAll(ids: Iterable<String>) {
        val available = ids.asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .toCollection(LinkedHashSet())
        if (available.isEmpty()) {
            deselectAll()
            return
        }
        selectedIds = if (available.all(selectedIds::contains) && selectedIds.size == available.size) {
            emptySet()
        } else {
            available
        }
    }

    fun retainAvailable(ids: Iterable<String>) {
        if (selectedIds.isEmpty()) return
        val available = ids.asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toHashSet()
        selectedIds = selectedIds.filterTo(linkedSetOf()) { it in available }
    }

    fun exit() {
        selectedIds = emptySet()
    }
}

@Composable
internal fun rememberTrackSelectionState(): TrackSelectionState = remember { TrackSelectionState() }

internal fun <T> TrackSelectionState.resolveSelected(
    available: Iterable<T>,
    keyOf: (T) -> String
): List<T> {
    if (selectedIds.isEmpty()) return emptyList()
    return available.filter { keyOf(it) in selectedIds }
}


internal fun trackSelectionKey(track: Track): String {
    track.isrc.trim().takeIf(String::isNotBlank)?.let {
        return "isrc:" + it.lowercase(Locale.ROOT)
    }
    track.id.trim().takeIf(String::isNotBlank)?.let {
        return "id:" + it.lowercase(Locale.ROOT)
    }
    track.audioVideoId.trim().takeIf(String::isNotBlank)?.let {
        return "audio:" + it.lowercase(Locale.ROOT)
    }
    track.counterpartVideoId.trim().takeIf(String::isNotBlank)?.let {
        return "video:" + it.lowercase(Locale.ROOT)
    }
    track.streamUrl.trim().takeIf(String::isNotBlank)?.let {
        return "stream:" + it
    }
    val title = track.title.trim().lowercase(Locale.ROOT)
    val artist = track.artist.trim().lowercase(Locale.ROOT)
    val durationBucket = track.durationMs.coerceAtLeast(0L) / 1_000L
    return "meta:" + title + "|" + artist + "|" + durationBucket
}
