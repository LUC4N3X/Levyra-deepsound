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
        audioVideoId = sourceVideoId
    )
}

internal fun buildYoutubeMusicPairingPayload(
    sourceVideoId: String,
    hl: String,
    gl: String,
    audioPrimary: Boolean
): JSONObject {
    val payload = JSONObject()
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
        .put("enablePersistentPlaylistPanel", true)
        .put("tunerSettingValue", "AUTOMIX_SETTING_NORMAL")
        .put("playlistId", "RDAMVM$sourceVideoId")

    if (audioPrimary) {
        payload
            .put("isAudioOnly", true)
            .put(
                "watchEndpointMusicSupportedConfigs",
                JSONObject().put(
                    "watchEndpointMusicConfig",
                    JSONObject()
                        .put("hasPersistentPlaylistPanel", true)
                        .put("musicVideoType", "MUSIC_VIDEO_TYPE_ATV")
                )
            )
    }

    return payload
}

internal class YoutubeMusicOfficialVideoResolver {
    private val client = LevyraHttpClientFactory.youtubePlayer()
    private val cache = ConcurrentHashMap<String, CachedCounterpart>()

    suspend fun resolve(
        track: Track,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): Track? = withContext(Dispatchers.IO) {
        val sourceVideoId = youtubeMusicAudioSourceId(track)
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
            requestCounterpart(
                sourceVideoId = sourceVideoId,
                hl = locale.hl,
                gl = locale.gl,
                audioPrimary = true
            ) ?: requestCounterpart(
                sourceVideoId = sourceVideoId,
                hl = locale.hl,
                gl = locale.gl,
                audioPrimary = false
            )
        }.getOrNull() ?: return@withContext null

        remember(cacheKey, counterpart, now)
        track.withOfficialCounterpart(sourceVideoId, counterpart)
    }

    private fun requestCounterpart(
        sourceVideoId: String,
        hl: String,
        gl: String,
        audioPrimary: Boolean
    ): YoutubeMusicWatchTrack? {
        val request = Request.Builder()
            .url("$YOUTUBE_MUSIC_ORIGIN/youtubei/v1/next?prettyPrint=false")
            .post(
                buildYoutubeMusicPairingPayload(sourceVideoId, hl, gl, audioPrimary)
                    .toString()
                    .toRequestBody(JSON_MEDIA_TYPE)
            )
            .header("Accept", "application/json")
            .header("X-Goog-Api-Format-Version", "1")
            .header("X-Origin", YOUTUBE_MUSIC_ORIGIN)
            .header("Referer", "$YOUTUBE_MUSIC_ORIGIN/")
            .header("User-Agent", WEB_USER_AGENT)
            .header("X-Youtube-Client-Name", WEB_REMIX_CLIENT_ID)
            .header("X-Youtube-Client-Version", WEB_REMIX_CLIENT_VERSION)
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val body = response.body ?: return@use null
            val declaredLength = body.contentLength()
            if (declaredLength > YOUTUBE_MUSIC_PAIRING_MAX_RESPONSE_BYTES) return@use null
            val responseBody = readUtf8Bounded(body.byteStream(), YOUTUBE_MUSIC_PAIRING_MAX_RESPONSE_BYTES)
                ?.takeIf { it.isNotBlank() }
                ?: return@use null
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
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val YOUTUBE_MUSIC_ORIGIN = "https://music.youtube.com"
        private const val WEB_REMIX_CLIENT_ID = "67"
        private const val WEB_REMIX_CLIENT_VERSION = "1.20260423.01.00"
        private const val WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"
        private const val YOUTUBE_MUSIC_PAIRING_MAX_RESPONSE_BYTES = 4L * 1024L * 1024L
        private const val CACHE_TTL_MS = 20L * 60L * 1000L
        private const val CACHE_MAX_ENTRIES = 96
    }
}
