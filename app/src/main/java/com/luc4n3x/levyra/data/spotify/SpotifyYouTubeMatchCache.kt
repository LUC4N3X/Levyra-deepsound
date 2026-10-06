package com.luc4n3x.levyra.data.spotify

import android.content.Context
import com.luc4n3x.levyra.domain.Track
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

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
}

internal class SpotifyYouTubeMatchCache private constructor(context: Context? = null) {
    private val memoryCache = SimpleLruCache<String, MatchedYouTubeData>(1_000)
    private val memoryFallback = ConcurrentHashMap<String, MatchedYouTubeData>()
    private val appContext = context?.applicationContext
    private val preferences = appContext?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun get(spotifyIdOrKey: String): MatchedYouTubeData? {
        val key = cleanKey(spotifyIdOrKey)
        if (key.isBlank()) return null

        memoryCache.get(key)?.let { return it }

        memoryFallback[key]?.let { return it }

        val prefs = preferences ?: return null
        val raw = prefs.getString(key, null) ?: return null
        val parsed = runCatching {
            val json = JSONObject(raw)
            val videoId = json.optString("videoId")
            val viewCount = json.optLong("viewCount", -1L)
            val videoUrl = json.optString("videoUrl")
            val audioVideoId = json.optString("audioVideoId")
            val videoType = json.optString("videoType")
            val timestamp = json.optLong("timestamp", 0L)
            MatchedYouTubeData(
                videoId = videoId,
                youtubeViewCount = viewCount,
                videoUrl = videoUrl,
                audioVideoId = audioVideoId,
                videoType = videoType,
                timestampMs = timestamp
            )
        }.getOrNull()

        if (parsed != null) {
            val now = System.currentTimeMillis()
            if (now - parsed.timestampMs < MATCH_TTL_MS) {
                memoryCache.put(key, parsed)
                return parsed
            }
        }
        return null
    }

    fun put(spotifyIdOrKey: String, youtubeTrack: Track) {
        val key = cleanKey(spotifyIdOrKey)
        val videoId = youtubeTrack.id.trim()
        if (key.isBlank() || videoId.isBlank()) return

        val data = MatchedYouTubeData(
            videoId = videoId,
            youtubeViewCount = youtubeTrack.youtubeViewCount,
            videoUrl = youtubeTrack.videoUrl.ifBlank { "https://www.youtube.com/watch?v=$videoId" },
            audioVideoId = youtubeTrack.audioVideoId.ifBlank { videoId },
            videoType = youtubeTrack.videoType,
            timestampMs = System.currentTimeMillis()
        )

        memoryCache.put(key, data)
        memoryFallback[key] = data

        preferences?.let { prefs ->
            runCatching {
                val json = JSONObject()
                    .put("videoId", data.videoId)
                    .put("viewCount", data.youtubeViewCount)
                    .put("videoUrl", data.videoUrl)
                    .put("audioVideoId", data.audioVideoId)
                    .put("videoType", data.videoType)
                    .put("timestamp", data.timestampMs)
                prefs.edit().putString(key, json.toString()).apply()
            }
        }
    }

    private fun cleanKey(key: String): String =
        key.trim().removePrefix("spotify:track:").removePrefix("spotify:")

    companion object {
        private const val PREFERENCES_NAME = "spotify_yt_match_cache"
        private const val MATCH_TTL_MS = 14L * 24L * 60L * 60L * 1000L

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
