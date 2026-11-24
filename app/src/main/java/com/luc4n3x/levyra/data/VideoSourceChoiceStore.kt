package com.luc4n3x.levyra.data

import android.content.SharedPreferences

internal data class VideoSourceChoice(val videoId: String, val videoType: String)

internal class VideoSourceChoiceStore(
    private val preferences: SharedPreferences,
    private val clock: () -> Long = System::currentTimeMillis,
    private val capacity: Int = DEFAULT_CAPACITY
) {
    fun get(trackKey: String): VideoSourceChoice? {
        if (trackKey.isBlank()) return null
        val fields = preferences.getString(KEY_PREFIX + trackKey, null)?.split(SEPARATOR) ?: return null
        val videoId = fields.getOrNull(0)?.takeIf(VIDEO_ID::matches) ?: return null
        return VideoSourceChoice(videoId, fields.getOrNull(1).orEmpty())
    }

    @Synchronized
    fun put(trackKey: String, choice: VideoSourceChoice) {
        if (trackKey.isBlank() || !VIDEO_ID.matches(choice.videoId)) return
        val editor = preferences.edit()
            .putString(KEY_PREFIX + trackKey, listOf(choice.videoId, choice.videoType, clock().toString()).joinToString(SEPARATOR))
        val stored = preferences.all.filterKeys { it.startsWith(KEY_PREFIX) && it != KEY_PREFIX + trackKey }
        val overflow = stored.size + 1 - capacity
        if (overflow > 0) {
            stored.entries
                .sortedBy { (_, value) -> (value as? String)?.substringAfterLast(SEPARATOR)?.toLongOrNull() ?: 0L }
                .take(overflow)
                .forEach { (key, _) -> editor.remove(key) }
        }
        editor.apply()
    }

    fun remove(trackKey: String) {
        if (trackKey.isBlank()) return
        preferences.edit().remove(KEY_PREFIX + trackKey).apply()
    }

    companion object {
        const val PREFERENCES = "levyra_video_source_choices"
        private const val KEY_PREFIX = "choice:"
        private const val SEPARATOR = "\t"
        private const val DEFAULT_CAPACITY = 500
        private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
    }
}
