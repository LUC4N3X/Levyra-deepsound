package com.luc4n3x.levyra.player.queue

import com.luc4n3x.levyra.domain.Track

internal const val RADIO_SESSION_MAX_ADDED_IDS = 120
internal const val RADIO_MIX_CONTEXT_TRACKS = 10

internal data class RadioSession(
    val spaceId: String,
    val seed: Track,
    val addedIds: Set<String> = emptySet(),
    val exhausted: Boolean = false
) {
    fun isActiveFor(snapshot: PlaybackQueueSnapshot): Boolean {
        if (exhausted || spaceId != snapshot.spaceId) return false
        val seedIdentity = playbackQueueIdentity(seed)
        return snapshot.tracks.any { track -> track.id in addedIds || playbackQueueIdentity(track) == seedIdentity }
    }

    fun recordAdded(ids: Collection<String>): RadioSession {
        if (ids.isEmpty()) return copy(exhausted = true)
        val merged = LinkedHashSet<String>(addedIds.size + ids.size)
        merged.addAll(addedIds)
        merged.addAll(ids)
        val bounded = if (merged.size > RADIO_SESSION_MAX_ADDED_IDS) {
            merged.toList().takeLast(RADIO_SESSION_MAX_ADDED_IDS).toSet()
        } else {
            merged
        }
        return copy(addedIds = bounded)
    }
}

internal fun radioSeedFor(session: RadioSession?, snapshot: PlaybackQueueSnapshot): Track? =
    session?.takeIf { it.isActiveFor(snapshot) }?.seed ?: snapshot.currentTrack

internal fun radioMixContext(snapshot: PlaybackQueueSnapshot, insertAfterCurrent: Boolean): List<Track> {
    val tracks = snapshot.tracks
    if (tracks.isEmpty()) return emptyList()
    val end = if (insertAfterCurrent) (snapshot.currentIndex + 1).coerceIn(0, tracks.size) else tracks.size
    return tracks.subList((end - RADIO_MIX_CONTEXT_TRACKS).coerceAtLeast(0), end).toList()
}

internal fun radioRecentlyPlayed(snapshot: PlaybackQueueSnapshot): List<Track> =
    snapshot.history.asSequence()
        .distinct()
        .mapNotNull(snapshot.tracks::getOrNull)
        .toList()
