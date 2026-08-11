package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.security.GoogleApiKeyHeaders
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

private val YOUTUBE_MUSIC_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

internal fun selectYoutubeMusicOfficialCounterpart(
    sourceVideoId: String,
    tracks: List<YoutubeMusicWatchTrack>
): YoutubeMusicWatchTrack? {
    val source = sourceVideoId.trim()
    if (source.isBlank()) return null

    return buildList {
        tracks.forEach { track ->
            if (track.videoId == source) track.counterpart?.let(::add)
            if (track.counterpart?.videoId == source) add(track)
        }
    }.firstOrNull { candidate ->
        YOUTUBE_MUSIC_VIDEO_ID.matches(candidate.videoId) &&
            candidate.videoId != source &&
            candidate.videoType.contains("OMV", ignoreCase = true)
    }
}

internal fun youtubeMusicAudioSourceId(track: Track): String {
    track.audioVideoId.trim()
        .takeIf(YOUTUBE_MUSIC_VIDEO_ID::matches)
        ?.let { return it }
    track.id.trim()
        .takeIf(YOUTUBE_MUSIC_VIDEO_ID::matches)
        ?.let { return it }
    return PlaybackSourceIdentity.sourceVideoId(track)
        .takeIf(YOUTUBE_MUSIC_VIDEO_ID::matches)
        .orEmpty()
}

internal fun youtubeMusicAudioPlaybackSeed(track: Track): Track? {
    val sourceVideoId = youtubeMusicAudioSourceId(track)
    if (sourceVideoId.isBlank()) return null
    return track.copy(
        streamUrl = "",
        videoStreamUrl = "",
        playbackManifest = null,
        videoUrl = "https://www.youtube.com/watch?v=$sourceVideoId",
        counterpartVideoId = "",
        videoType = "MUSIC_VIDEO_TYPE_ATV",
        audioVideoId = sourceVideoId
    )
}

internal fun buildYoutubeMusicPairingPayload(
    sourceVideoId: String,
    hl: String,
    gl: String
): JSONObject = JSONObject()
    .put("videoId", sourceVideoId)
    .put(
        "context",
        JSONObject()
            .put(
                "client",
                JSONObject()
                    .put("clientName", "WEB_REMIX")
                    .put("clientVersion", "1.20260423.01.00")
                    .put("hl", hl)
                    .put("gl", gl)
                    .put("platform", "DESKTOP")
            )
            .put("user", JSONObject())
    )

internal fun youtubeMusicPairingEndpoint(apiKey: String): String {
    require(apiKey.isNotBlank()) { "YouTube Innertube API key assente" }
    return "$YOUTUBE_MUSIC_ORIGIN/youtubei/v1/next?key=$apiKey&prettyPrint=false"
}

internal class YoutubeMusicOfficialVideoResolver {
    private val apiKey = BuildConfig.YOUTUBE_INNERTUBE_API_KEY
    private val client = LevyraHttpClientFactory.youtubePlayer()

    suspend fun resolve(
        track: Track,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): Track? = withContext(Dispatchers.IO) {
        val sourceVideoId = youtubeMusicAudioSourceId(track)
        if (sourceVideoId.isBlank()) return@withContext null

        val locale = LevyraContentLocales.forLanguage(languageCode)
        val cacheKey = "$sourceVideoId|${locale.gl.lowercase(Locale.ROOT)}"
        val now = System.currentTimeMillis()
        officialCounterpartCache[cacheKey]?.let { cached ->
            if (cached.expiresAtMs > now) {
                return@withContext track.withOfficialCounterpart(sourceVideoId, cached.counterpart)
            }
            officialCounterpartCache.remove(cacheKey, cached)
        }

        val counterpart = requestCounterpart(
            sourceVideoId = sourceVideoId,
            hl = locale.hl,
            gl = locale.gl
        ) ?: return@withContext null

        remember(cacheKey, counterpart, now)
        track.withOfficialCounterpart(sourceVideoId, counterpart)
    }

    private fun requestCounterpart(
        sourceVideoId: String,
        hl: String,
        gl: String
    ): YoutubeMusicWatchTrack? {
        val requestBuilder = Request.Builder()
            .url(youtubeMusicPairingEndpoint(apiKey))
            .post(
                buildYoutubeMusicPairingPayload(sourceVideoId, hl, gl)
                    .toString()
                    .toRequestBody(JSON_MEDIA_TYPE)
            )
            .header("Accept", "application/json")
            .header("Origin", YOUTUBE_MUSIC_ORIGIN)
            .header("X-Origin", YOUTUBE_MUSIC_ORIGIN)
            .header("X-Goog-Api-Format-Version", "1")
            .header("Referer", "$YOUTUBE_MUSIC_ORIGIN/")
            .header("User-Agent", WEB_USER_AGENT)
            .header("X-Youtube-Client-Name", WEB_REMIX_CLIENT_ID)
            .header("X-Youtube-Client-Version", WEB_REMIX_CLIENT_VERSION)
        GoogleApiKeyHeaders.applyTo(requestBuilder, null)

        return client.newCall(requestBuilder.build()).execute().use { response ->
            val body = response.body
            val declaredLength = body?.contentLength() ?: 0L
            if (declaredLength > YOUTUBE_MUSIC_PAIRING_MAX_RESPONSE_BYTES) {
                throw YoutubeMusicRequestException("next", response.code, "response too large")
            }
            val responseBody = body
                ?.let { readUtf8Bounded(it.byteStream(), YOUTUBE_MUSIC_PAIRING_MAX_RESPONSE_BYTES) }
                .orEmpty()
            if (!response.isSuccessful) {
                throw YoutubeMusicRequestException("next", response.code, responseBody.take(512))
            }
            if (responseBody.isBlank()) return@use null
            val playlist = YoutubeMusicWatchParser.parseWatchPlaylist(JSONObject(responseBody))
            selectYoutubeMusicOfficialCounterpart(sourceVideoId, playlist.tracks)
        }
    }

    private fun Track.withOfficialCounterpart(
        sourceVideoId: String,
        counterpart: YoutubeMusicWatchTrack
    ): Track {
        return copy(
            streamUrl = "",
            videoStreamUrl = "",
            playbackManifest = null,
            videoUrl = "https://www.youtube.com/watch?v=${counterpart.videoId}",
            counterpartVideoId = counterpart.videoId,
            videoType = counterpart.videoType,
            audioVideoId = audioVideoId.trim().takeIf(YOUTUBE_MUSIC_VIDEO_ID::matches).orEmpty().ifBlank { sourceVideoId }
        )
    }

    private fun remember(key: String, counterpart: YoutubeMusicWatchTrack, now: Long) {
        officialCounterpartCache[key] = CachedCounterpart(counterpart, now + CACHE_TTL_MS)
        if (officialCounterpartCache.size <= CACHE_MAX_ENTRIES) return

        officialCounterpartCache.keys.toList().forEach { candidateKey ->
            val cached = officialCounterpartCache[candidateKey] ?: return@forEach
            if (cached.expiresAtMs <= now) officialCounterpartCache.remove(candidateKey, cached)
        }
        if (officialCounterpartCache.size > CACHE_MAX_ENTRIES) officialCounterpartCache.clear()
    }

    private data class CachedCounterpart(
        val counterpart: YoutubeMusicWatchTrack,
        val expiresAtMs: Long
    )

    private companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val WEB_REMIX_CLIENT_ID = "67"
        private const val WEB_REMIX_CLIENT_VERSION = "1.20260423.01.00"
        private const val WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"
        private const val YOUTUBE_MUSIC_PAIRING_MAX_RESPONSE_BYTES = 4L * 1024L * 1024L
        private const val CACHE_TTL_MS = 20L * 60L * 1000L
        private const val CACHE_MAX_ENTRIES = 96
        private val officialCounterpartCache = ConcurrentHashMap<String, CachedCounterpart>()
    }
}

private const val YOUTUBE_MUSIC_ORIGIN = "https://music.youtube.com"
