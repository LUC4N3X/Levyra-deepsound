package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal fun selectYoutubeMusicOfficialCounterpart(
    sourceVideoId: String,
    tracks: List<YoutubeMusicWatchTrack>
): YoutubeMusicWatchTrack? {
    val source = sourceVideoId.trim()
    if (source.isBlank()) return null

    val paired = buildList {
        tracks.forEach { track ->
            if (track.videoId == source) track.counterpart?.let(::add)
            if (track.counterpart?.videoId == source) add(track)
        }
    }.filter { candidate ->
        candidate.videoId.isNotBlank() &&
            candidate.videoId != source &&
            !candidate.videoType.contains("ATV", ignoreCase = true)
    }

    return paired.maxByOrNull { candidate ->
        when {
            candidate.videoType.contains("OMV", ignoreCase = true) -> 2
            candidate.videoType.contains("UGC", ignoreCase = true) -> 1
            else -> 0
        }
    }
}

internal class YoutubeMusicOfficialVideoResolver {
    private val client = LevyraHttpClientFactory.youtubePlayer()
    private val cache = ConcurrentHashMap<String, CachedCounterpart>()

    suspend fun resolve(
        track: Track,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): Track? = withContext(Dispatchers.IO) {
        val sourceVideoId = audioVideoId(track)
        if (sourceVideoId.isBlank()) return@withContext null

        val locale = LevyraContentLocales.forLanguage(languageCode)
        val cacheKey = "$sourceVideoId|${locale.gl.lowercase(Locale.ROOT)}"
        val now = System.currentTimeMillis()
        cache[cacheKey]?.let { cached ->
            if (cached.expiresAtMs > now) {
                return@withContext track.withOfficialCounterpart(sourceVideoId, cached.counterpart)
            }
            cache.remove(cacheKey, cached)
        }

        val counterpart = runCatchingPreservingCancellation {
            val payload = JSONObject()
                .put("videoId", sourceVideoId)
                .put(
                    "context",
                    JSONObject()
                        .put(
                            "client",
                            JSONObject()
                                .put("clientName", WEB_REMIX_CLIENT_NAME)
                                .put("clientVersion", WEB_REMIX_CLIENT_VERSION)
                                .put("hl", locale.hl)
                                .put("gl", locale.gl)
                                .put("platform", "DESKTOP")
                        )
                        .put("user", JSONObject())
                )
            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/next?prettyPrint=false")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .header("Accept-Encoding", "br,gzip")
                .header("X-Goog-Api-Format-Version", "1")
                .header("X-Origin", YOUTUBE_MUSIC_ORIGIN)
                .header("Referer", "$YOUTUBE_MUSIC_ORIGIN/")
                .header("User-Agent", WEB_USER_AGENT)
                .header("X-Youtube-Client-Name", WEB_REMIX_CLIENT_ID)
                .header("X-Youtube-Client-Version", WEB_REMIX_CLIENT_VERSION)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful || responseBody.isBlank()) return@use null
                val playlist = YoutubeMusicWatchParser.parseWatchPlaylist(JSONObject(responseBody))
                selectYoutubeMusicOfficialCounterpart(sourceVideoId, playlist.tracks)
            }
        }.getOrNull() ?: return@withContext null

        remember(cacheKey, counterpart, now)
        track.withOfficialCounterpart(sourceVideoId, counterpart)
    }

    private fun audioVideoId(track: Track): String {
        track.audioVideoId.trim()
            .takeIf(YOUTUBE_VIDEO_ID::matches)
            ?.let { return it }
        return PlaybackSourceIdentity.sourceVideoId(track)
            .takeIf(YOUTUBE_VIDEO_ID::matches)
            .orEmpty()
    }

    private fun Track.withOfficialCounterpart(
        sourceVideoId: String,
        counterpart: YoutubeMusicWatchTrack
    ): Track {
        return copy(
            videoUrl = "https://www.youtube.com/watch?v=${counterpart.videoId}",
            counterpartVideoId = counterpart.videoId,
            videoType = counterpart.videoType,
            audioVideoId = audioVideoId.trim().takeIf(YOUTUBE_VIDEO_ID::matches).orEmpty().ifBlank { sourceVideoId }
        )
    }

    private fun remember(key: String, counterpart: YoutubeMusicWatchTrack, now: Long) {
        cache[key] = CachedCounterpart(counterpart, now + CACHE_TTL_MS)
        if (cache.size <= CACHE_MAX_ENTRIES) return

        cache.keys.toList().forEach { candidateKey ->
            val cached = cache[candidateKey] ?: return@forEach
            if (cached.expiresAtMs <= now) cache.remove(candidateKey, cached)
        }
        if (cache.size > CACHE_MAX_ENTRIES) cache.clear()
    }

    private data class CachedCounterpart(
        val counterpart: YoutubeMusicWatchTrack,
        val expiresAtMs: Long
    )

    private companion object {
        private val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val YOUTUBE_MUSIC_ORIGIN = "https://music.youtube.com"
        private const val WEB_REMIX_CLIENT_NAME = "WEB_REMIX"
        private const val WEB_REMIX_CLIENT_ID = "67"
        private const val WEB_REMIX_CLIENT_VERSION = "1.20260423.01.00"
        private const val WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"
        private const val CACHE_TTL_MS = 20L * 60L * 1000L
        private const val CACHE_MAX_ENTRIES = 96
    }
}
