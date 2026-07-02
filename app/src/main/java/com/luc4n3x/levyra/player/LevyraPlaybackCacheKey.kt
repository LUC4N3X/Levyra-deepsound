package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.Track

object LevyraPlaybackCacheKey {
    fun stream(track: Track): String {
        val id = stableId(track)
        val signature = track.streamUrl.hashCode().toUInt().toString(16)
        return "levyra:$id:stream:$signature"
    }

    fun video(track: Track): String {
        val id = stableId(track)
        val signature = track.videoStreamUrl.hashCode().toUInt().toString(16)
        return "levyra:$id:video:$signature"
    }

    private fun stableId(track: Track): String = track.id.trim()
        .ifBlank { track.videoUrl.trim() }
        .ifBlank { track.title.trim() }
}
