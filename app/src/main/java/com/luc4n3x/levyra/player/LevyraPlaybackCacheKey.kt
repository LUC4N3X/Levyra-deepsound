package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.Track

object LevyraPlaybackCacheKey {
    fun stream(track: Track): String {
        val id = track.id.trim().ifBlank { track.videoUrl.trim() }.ifBlank { track.title.trim() }
        val signature = track.streamUrl.hashCode().toUInt().toString(16)
        return "levyra:$id:$signature"
    }
}
