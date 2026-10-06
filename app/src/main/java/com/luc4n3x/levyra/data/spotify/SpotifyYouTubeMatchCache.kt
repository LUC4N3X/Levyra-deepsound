package com.luc4n3x.levyra.data.spotify

import android.content.Context
import com.luc4n3x.levyra.domain.Track
import org.json.JSONObject

internal data class MatchedYouTubeData(
    val videoId: String,
    val youtubeViewCount: Long,
    val videoUrl: String,
    val audioVideoId: String,
    val videoType: String,
    val timestampMs: Long
)

private class SimpleLruCache<K, V>(private val maxSize: Int) {
    private val map = object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun get(key: K): V? = map[key]

    @Synchronized
    fun put(key: K, value: V) {
        map[key] = value
    }

    @Synchronized
    fun remove(key: K) {
        map.remove(key)
    }
}

internal class SpotifyYouTubeMatchCache private constructor(context: Context? = null) {
    private val memoryCache = SimpleLruCache<String, MatchedYouTubeData>(MAX_MEMORY_ENTRIES)
    private val appContext = context?.applicationContext
    private val preferences = appContext?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun get(spotifyIdOrKey: String): MatchedYouTubeData? {
        val key = cleanKey(spotifyIdOrKey)
        if (key.isBlank()) return null
        val now = System.currentTimeMillis()

        memoryCache.get(key)?.let { cached ->
            if (isFresh(cached, now)) return cached
            memoryCache.remove(key)
        }

        val prefs = preferences ?: return null
        val raw = prefs.getString(key, null) ?: return null
        val parsed = runCatching { decode(raw) }.getOrNull()
        if (parsed != null && isFresh(parsed, now)) {
            memoryCache.put(key, parsed)
            return parsed
        }

        prefs.edit().remove(key).apply()
        return null
    }

    fun put(spotifyIdOrKey: String, youtubeTrack: Track) {
        val key = cleanKey(spotifyIdOrKey)
        val videoId = youtubeTrack.id.trim()
        if (key.isBlank() || !YOUTUBE_VIDEO_ID.matches(videoId)) return

        val audioVideoId = youtubeTrack.audioVideoId
            .trim()
            .takeIf(YOUTUBE_VIDEO_ID::matches)
            .orEmpty()
            .ifBlank { videoId }
        val data = MatchedYouTubeData(
            videoId = videoId,
            youtubeViewCount = youtubeTrack.youtubeViewCount,
            videoUrl = youtubeTrack.videoUrl.ifBlank { "https://www.youtube.com/watch?v=$videoId" },
            audioVideoId = audioVideoId,
            videoType = youtubeTrack.videoType,
            timestampMs = System.currentTimeMillis()
        )

        memoryCache.put(key, data)
        preferences?.edit()?.putString(key, encode(data))?.apply()
    }

    private fun decode(raw: String): MatchedYouTubeData {
        val json = JSONObject(raw)
        return MatchedYouTubeData(
            videoId = json.optString("videoId").trim(),
            youtubeViewCount = json.optLong("viewCount", -1L),
            videoUrl = json.optString("videoUrl"),
            audioVideoId = json.optString("audioVideoId").trim(),
            videoType = json.optString("videoType"),
            timestampMs = json.optLong("timestamp", 0L)
        )
    }

    private fun encode(data: MatchedYouTubeData): String =
        JSONObject()
            .put("videoId", data.videoId)
            .put("viewCount", data.youtubeViewCount)
            .put("videoUrl", data.videoUrl)
            .put("audioVideoId", data.audioVideoId)
            .put("videoType", data.videoType)
            .put("timestamp", data.timestampMs)
            .toString()

    private fun isFresh(data: MatchedYouTubeData, now: Long): Boolean {
        if (!YOUTUBE_VIDEO_ID.matches(data.videoId)) return false
        val age = now - data.timestampMs
        return age in 0 until MATCH_TTL_MS
    }

    private fun cleanKey(key: String): String =
        key.trim().removePrefix("spotify:track:").removePrefix("spotify:")

    companion object {
        private const val PREFERENCES_NAME = "spotify_yt_match_cache"
        private const val MAX_MEMORY_ENTRIES = 1_000
        private const val MATCH_TTL_MS = 14L * 24L * 60L * 60L * 1000L
        private val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

        @Volatile
        private var instance: SpotifyYouTubeMatchCache? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = SpotifyYouTubeMatchCache(context.applicationContext)
                    }
                }
            }
        }

        fun get(context: Context? = null): SpotifyYouTubeMatchCache {
            return instance ?: synchronized(this) {
                instance ?: SpotifyYouTubeMatchCache(context).also { instance = it }
            }
        }
    }
}
