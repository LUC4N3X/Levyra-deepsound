package com.luc4n3x.levyra.player

import android.content.Context
import android.content.SharedPreferences
import com.luc4n3x.levyra.data.PlaybackSourceIdentity
import com.luc4n3x.levyra.domain.Track
import java.net.URLDecoder
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

internal data class PlaybackCacheHint(
    val sourceVideoId: String,
    val cacheKey: String,
    val mimeType: String,
    val updatedAtMs: Long
)

internal data class PlaybackCacheReadSpec(
    val cacheKey: String,
    val mimeType: String
)

internal object PlaybackCacheHintStore {
    private const val PREFS_NAME = "levyra.playback.cache.hints"
    private const val KEY_HINTS = "recent"
    private const val MAX_HINTS = 16
    private const val MAX_CACHE_KEY_LENGTH = 512

    private val lock = Any()
    private val hints = LinkedHashMap<String, PlaybackCacheHint>()

    @Volatile
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        if (preferences != null) return
        synchronized(lock) {
            if (preferences != null) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            hints.clear()
            decode(prefs.getString(KEY_HINTS, null)).forEach { hint ->
                hints[hint.sourceVideoId] = hint
            }
            trimLocked()
            preferences = prefs
        }
    }

    fun record(track: Track, cacheKey: String, mimeType: String) {
        val sourceVideoId = sourceVideoId(track)
        val cleanKey = cacheKey.trim()
        if (preferences == null || sourceVideoId.isBlank() || cleanKey.isBlank() || cleanKey.length > MAX_CACHE_KEY_LENGTH) return
        val hint = PlaybackCacheHint(
            sourceVideoId = sourceVideoId,
            cacheKey = cleanKey,
            mimeType = mimeType.substringBefore(';').trim(),
            updatedAtMs = System.currentTimeMillis()
        )
        synchronized(lock) {
            hints.remove(sourceVideoId)
            hints[sourceVideoId] = hint
            trimLocked()
            persistLocked()
        }
    }

    fun find(track: Track): PlaybackCacheHint? {
        val sourceVideoId = sourceVideoId(track)
        if (sourceVideoId.isBlank()) return null
        return synchronized(lock) { hints[sourceVideoId] }
    }

    private fun sourceVideoId(track: Track): String =
        PlaybackSourceIdentity.sourceVideoId(track).trim()

    private fun trimLocked() {
        while (hints.size > MAX_HINTS) {
            val oldest = hints.entries.firstOrNull()?.key ?: break
            hints.remove(oldest)
        }
    }

    private fun persistLocked() {
        val prefs = preferences ?: return
        val array = JSONArray()
        hints.values.forEach { hint ->
            array.put(
                JSONObject()
                    .put("sourceVideoId", hint.sourceVideoId)
                    .put("cacheKey", hint.cacheKey)
                    .put("mimeType", hint.mimeType)
                    .put("updatedAtMs", hint.updatedAtMs)
            )
        }
        prefs.edit().putString(KEY_HINTS, array.toString()).apply()
    }

    private fun decode(raw: String?): List<PlaybackCacheHint> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val sourceVideoId = item.optString("sourceVideoId").trim()
                    val cacheKey = item.optString("cacheKey").trim()
                    if (sourceVideoId.isBlank() || cacheKey.isBlank() || cacheKey.length > MAX_CACHE_KEY_LENGTH) continue
                    add(
                        PlaybackCacheHint(
                            sourceVideoId = sourceVideoId,
                            cacheKey = cacheKey,
                            mimeType = item.optString("mimeType").substringBefore(';').trim(),
                            updatedAtMs = item.optLong("updatedAtMs", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}

private const val PLAYBACK_CACHE_ONLY_PREFIX = "levyra-cache://media?"

internal fun playbackCacheOnlyUri(hint: PlaybackCacheHint): String =
    buildString {
        append(PLAYBACK_CACHE_ONLY_PREFIX)
        append("key=")
        append(encodePlaybackCacheComponent(hint.cacheKey))
        append("&mime=")
        append(encodePlaybackCacheComponent(hint.mimeType))
    }

internal fun playbackCacheReadSpec(url: String): PlaybackCacheReadSpec? {
    if (!url.startsWith(PLAYBACK_CACHE_ONLY_PREFIX, ignoreCase = true)) return null
    val query = url.substringAfter('?', "")
    var cacheKey = ""
    var mimeType = ""
    query.split('&').forEach { part ->
        val name = part.substringBefore('=', "")
        val value = part.substringAfter('=', "")
        when (name) {
            "key" -> cacheKey = decodePlaybackCacheComponent(value)
            "mime" -> mimeType = decodePlaybackCacheComponent(value)
        }
    }
    if (cacheKey.isBlank()) return null
    return PlaybackCacheReadSpec(cacheKey = cacheKey, mimeType = mimeType)
}

internal fun shouldRememberPlaybackCacheHint(
    streamUrl: String,
    mimeType: String?,
    videoMode: Boolean
): Boolean {
    if (videoMode) return false
    val clean = streamUrl.trim().lowercase()
    if (!clean.startsWith("https://") && !clean.startsWith("http://")) return false
    val normalizedMime = mimeType.orEmpty().substringBefore(';').trim().lowercase()
    if (!normalizedMime.startsWith("audio/")) return false
    val path = clean.substringBefore('?').substringBefore('#')
    return !path.endsWith(".m3u8") &&
        !path.endsWith(".mpd") &&
        !path.contains("/hls_playlist") &&
        !path.contains("/manifest/hls")
}

private fun encodePlaybackCacheComponent(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8.name())

private fun decodePlaybackCacheComponent(value: String): String =
    runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrDefault("")
