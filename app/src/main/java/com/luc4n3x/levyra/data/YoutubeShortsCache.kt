package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.Track
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.Locale

private const val SHORTS_CACHE_NAME = "levyra_shorts_cache"
private const val SHORTS_CACHE_LIMIT = 24
private const val SHORTS_CACHE_MAX_AGE_MS = 6L * 60L * 60L * 1_000L

internal data class YoutubeShortsCacheSnapshot(
    val tracks: List<Track>,
    val savedAtMs: Long
) {
    fun isFresh(nowMs: Long = System.currentTimeMillis()): Boolean =
        tracks.isNotEmpty() && savedAtMs > 0L && nowMs - savedAtMs <= SHORTS_CACHE_MAX_AGE_MS
}

internal class YoutubeShortsCache(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        SHORTS_CACHE_NAME,
        Context.MODE_PRIVATE
    )

    fun load(languageCode: String, profileSignature: String = ""): YoutubeShortsCacheSnapshot {
        val wantedProfile = profileSignature.trim()
        if (wantedProfile.isBlank()) return YoutubeShortsCacheSnapshot(emptyList(), 0L)
        val raw = preferences.getString(cacheKey(languageCode), null).orEmpty()
        if (raw.isBlank()) return YoutubeShortsCacheSnapshot(emptyList(), 0L)
        return runCatching {
            val root = JSONObject(raw)
            if (root.optString("profile") != profileFingerprint(wantedProfile)) {
                YoutubeShortsCacheSnapshot(emptyList(), 0L)
            } else {
                val items = root.optJSONArray("tracks") ?: JSONArray()
                val tracks = buildList {
                    for (index in 0 until items.length()) {
                        items.optJSONObject(index)
                            ?.let(TrackJson::fromJson)
                            ?.takeIf(::isYoutubeShortTrack)
                            ?.let(::add)
                    }
                }
                    .distinctBy { track -> track.id }
                    .take(SHORTS_CACHE_LIMIT)
                YoutubeShortsCacheSnapshot(
                    tracks = tracks,
                    savedAtMs = root.optLong("savedAtMs", 0L)
                )
            }
        }.getOrElse { error ->
            Timber.w(error, "Unable to read Shorts cache for %s", languageCode)
            YoutubeShortsCacheSnapshot(emptyList(), 0L)
        }
    }

    fun save(
        languageCode: String,
        tracks: List<Track>,
        savedAtMs: Long = System.currentTimeMillis(),
        profileSignature: String = ""
    ) {
        val verified = tracks
            .asSequence()
            .filter(::isYoutubeShortTrack)
            .distinctBy { track -> track.id }
            .take(SHORTS_CACHE_LIMIT)
            .toList()
        if (verified.isEmpty()) return
        val array = JSONArray().apply {
            verified.forEach { track -> put(TrackJson.toJson(track)) }
        }
        val root = JSONObject()
            .put("savedAtMs", savedAtMs.coerceAtLeast(0L))
            .put("profile", profileFingerprint(profileSignature))
            .put("tracks", array)
        preferences.edit().putString(cacheKey(languageCode), root.toString()).apply()
    }

    private fun cacheKey(languageCode: String): String =
        "shorts_${LevyraLanguageCatalog.normalize(languageCode)}"

    private fun profileFingerprint(profileSignature: String): String {
        val normalized = profileSignature.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return "default"
        return normalized.hashCode().toUInt().toString(16)
    }
}
