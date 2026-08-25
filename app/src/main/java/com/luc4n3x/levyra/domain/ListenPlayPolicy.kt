package com.luc4n3x.levyra.domain

object ListenPlayPolicy {

    const val MIN_EVENT_MS = 5_000L
    const val COUNTED_PLAY_MS = 30_000L
    const val SHORT_TRACK_COMPLETION_NUMERATOR = 8L
    const val SHORT_TRACK_COMPLETION_DENOMINATOR = 10L

    fun isRecordableEvent(listenedMs: Long): Boolean = listenedMs >= MIN_EVENT_MS

    fun isCountedPlay(listenedMs: Long, trackDurationMs: Long, completed: Boolean): Boolean {
        if (listenedMs < MIN_EVENT_MS) return false
        if (listenedMs >= COUNTED_PLAY_MS) return true
        if (!completed) return false
        if (trackDurationMs <= 0L || trackDurationMs >= COUNTED_PLAY_MS) return false
        return listenedMs * SHORT_TRACK_COMPLETION_DENOMINATOR >=
            trackDurationMs * SHORT_TRACK_COMPLETION_NUMERATOR
    }

    fun isCountedPlay(event: ListenEvent): Boolean =
        isCountedPlay(event.listenedMs, event.trackDurationMs, event.completed)
}

object ListenIdentity {

    fun trackKey(trackId: String, title: String, artist: String): String {
        val id = trackId.trim()
        if (id.isNotEmpty()) return id
        return "${title.trim().lowercase()}|${artist.trim().lowercase()}"
    }

    fun trackKey(event: ListenEvent): String = trackKey(event.trackId, event.title, event.artist)

    fun artistKey(artist: String): String = artist.trim().lowercase()
}
