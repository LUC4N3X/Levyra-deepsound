package com.luc4n3x.levyra.player.offline

import com.luc4n3x.levyra.data.LyricsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

/**
 * Offline-only forced refresh wrapper.
 *
 * Normal lyrics observation keeps the repository's existing cache/quality rules. When an
 * export explicitly asks for a refresh, this wrapper only goes around those rules when the
 * current cache is missing or contains plain lyrics, so a complete synced version can be
 * embedded without changing the repository behavior used by playback/UI.
 */
internal fun LyricsRepository.observe(
    title: String,
    artist: String,
    durationSec: Long,
    album: String = "",
    videoId: String = "",
    languageCode: String = "",
    translate: Boolean = false,
    forceRefresh: Boolean
): Flow<LyricsRepository.LyricsResult> {
    if (!forceRefresh) {
        return observe(title, artist, durationSec, album, videoId, languageCode, translate)
    }

    return flow {
        var latest: LyricsRepository.LyricsResult? = null
        observe(title, artist, durationSec, album, videoId, languageCode, translate).collect { result ->
            latest = result
            emit(result)
        }

        val existing = latest
        if (existing?.manualSelection == true || existing.hasCompleteSyncedTiming()) {
            return@flow
        }

        val versions = versions(title, artist, durationSec, album, videoId, languageCode, translate)
        val selected = versions.firstOrNull { it.selected }?.result
        val synced = versions.asSequence()
            .map { it.result }
            .filter { it.hasCompleteSyncedTiming() }
            .maxByOrNull { it.confidence }
        val fallback = if (existing == null) {
            versions.asSequence().map { it.result }.maxByOrNull { it.confidence }
        } else {
            null
        }
        val preferred = selected ?: synced ?: fallback ?: return@flow

        if (preferred != existing) emit(preferred.copy(cached = false))
    }
}

private fun LyricsRepository.LyricsResult?.hasCompleteSyncedTiming(): Boolean {
    val result = this ?: return false
    if (!result.synced) return false
    val lyricLines = result.lines.filterNot { line ->
        line.isMetadata || line.isInstrumental || line.text.isBlank()
    }
    return lyricLines.isNotEmpty() && lyricLines.all { it.startMs >= 0L }
}
