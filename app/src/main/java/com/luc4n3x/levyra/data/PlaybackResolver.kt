package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class PlaybackResolver private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var instance: PlaybackResolver? = null

        fun getInstance(context: Context): PlaybackResolver {
            return instance ?: synchronized(this) {
                instance ?: PlaybackResolver(context.applicationContext).also { instance = it }
            }
        }
    }

    private val apiKey = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private val prefs = context.getSharedPreferences("levyra_stream_cache", Context.MODE_PRIVATE)
    private val streamCache = ConcurrentHashMap<String, CachedStream>()
    private val inFlight = ConcurrentHashMap<String, Deferred<Track>>()
    private val fallbackTtlMs = 90L * 60L * 1000L
    private val maxTtlMs = 5L * 60L * 60L * 1000L
    private val resolveTimeoutMs = 14_000L

    private val profiles = listOf(
        ClientProfile("ANDROID_MUSIC", "8.10.52", "Android Music", "Mozilla/5.0 (Linux; Android 15; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) com.google.android.apps.youtube.music/8.10.52", true, 0L),
        ClientProfile("ANDROID", "19.44.38", "Android", "com.google.android.youtube/19.44.38 (Linux; U; Android 15)", true, 50L),
        ClientProfile("IOS", "20.10.4", "iOS", "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3 like Mac OS X; it_IT)", false, 100L),
        ClientProfile("WEB_REMIX", "1.20260423.01.00", "YouTube Music Web", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36", false, 180L),
        ClientProfile("WEB_EMBEDDED_PLAYER", "1.20260423.01.00", "Embedded Player", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36", false, 240L)
    )

    init {
        restoreCache()
    }

    fun cached(track: Track): Track? {
        if (track.streamUrl.isNotBlank()) {
            return if (streamStillFresh(track.streamUrl)) track else null
        }
        val key = cacheKey(track)
        val hit = streamCache[key] ?: return null
        if (!isFresh(hit.expiresAt)) {
            remove(key)
            return null
        }
        return hit.track
    }

    suspend fun resolve(track: Track): Track = coroutineScope {
        track.streamUrl.takeIf { it.isNotBlank() && streamStillFresh(it) }?.let { return@coroutineScope track }
        cached(track)?.let { return@coroutineScope it }

        val key = cacheKey(track)
        val deferred = async(Dispatchers.IO, start = CoroutineStart.LAZY) {
            withTimeout(resolveTimeoutMs) {
                resolveUncached(track.copy(streamUrl = ""))
            }
        }
        val previous = inFlight.putIfAbsent(key, deferred)
        if (previous != null) return@coroutineScope previous.await()

        try {
            deferred.start()
            return@coroutineScope deferred.await()
        } finally {
            inFlight.remove(key, deferred)
        }
    }

    suspend fun prefetch(track: Track) {
        if (track.streamUrl.isNotBlank()) {
            if (streamStillFresh(track.streamUrl)) store(track)
            return
        }
        if (cached(track) != null) return
        runCatching { resolve(track) }
    }

    private suspend fun resolveUncached(track: Track): Track = withContext(Dispatchers.IO) {
        val errors = Collections.synchronizedList(mutableListOf<String>())
        val stream = raceInnerTube(track, errors)
        if (stream != null) {
            val resolved = track.copy(
                streamUrl = stream.url,
                durationMs = stream.durationMs.takeIf { it > 0L } ?: track.durationMs,
                thumbnailUrl = stream.thumbnailUrl.ifBlank { track.thumbnailUrl },
                largeThumbnailUrl = stream.thumbnailUrl.ifBlank { track.largeThumbnailUrl },
                source = stream.source
            )
            store(resolved)
            return@withContext resolved
        }

        val newPipe = runCatching { resolveWithNewPipe(track) }
        if (newPipe.isSuccess) {
            val resolved = newPipe.getOrThrow()
            store(resolved)
            return@withContext resolved
        }
        newPipe.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }?.let { errors += "NewPipe: $it" }

        val reason = errors.firstOrNull { it.contains("age", true) || it.contains("anonymous", true) || it.contains("login", true) }
            ?: errors.firstOrNull()
            ?: "Stream non disponibile"
        throw PlaybackBlockedException(reason)
    }

    private suspend fun raceInnerTube(track: Track, errors: MutableList<String>): DirectStream? = coroutineScope {
        val winner = CompletableDeferred<DirectStream?>()
        val workers = profiles.map { profile ->
            launch {
                if (profile.delayMs > 0L) delay(profile.delayMs)
                val attempt = runCatching { resolveWithInnerTube(track, profile) }
                attempt.onSuccess { stream ->
                    if (stream.url.isNotBlank()) winner.complete(stream)
                }.onFailure { error ->
                    error.message?.takeIf { it.isNotBlank() }?.let { errors += "${profile.label}: $it" }
                }
            }
        }
        launch {
            workers.joinAll()
            winner.complete(null)
        }
        val result = winner.await()
        coroutineContext.cancelChildren()
        result
    }

    private fun restoreCache() {
        runCatching {
            val now = System.currentTimeMillis()
            val editor = prefs.edit()
            var modified = false
            prefs.all.forEach { (key, value) ->
                val raw = value as? String ?: return@forEach
                val json = runCatching { JSONObject(raw) }.getOrNull() ?: return@forEach
                val streamUrl = json.optString("streamUrl")
                val expiresAt = json.optLong("expiresAt", json.optLong("at", 0L) + fallbackTtlMs)
                val track = json.optJSONObject("track")?.let(TrackJson::fromJson)?.copy(streamUrl = streamUrl)
                if (track != null && streamUrl.isNotBlank() && now < expiresAt && streamStillFresh(streamUrl)) {
                    streamCache[key] = CachedStream(track, expiresAt)
                } else {
                    editor.remove(key)
                    modified = true
                }
            }
            if (modified) editor.apply()
        }
    }

    private fun store(track: Track) {
        if (track.streamUrl.isBlank() || !streamStillFresh(track.streamUrl)) return
        val key = cacheKey(track)
        val expiresAt = expiresAtFor(track.streamUrl)
        streamCache[key] = CachedStream(track, expiresAt)
        val json = JSONObject()
            .put("expiresAt", expiresAt)
            .put("streamUrl", track.streamUrl)
            .put("track", TrackJson.toJson(track.copy(streamUrl = "")))
        prefs.edit().putString(key, json.toString()).apply()
    }

    private fun remove(key: String) {
        streamCache.remove(key)
        prefs.edit().remove(key).apply()
    }

    private fun isFresh(expiresAt: Long): Boolean = System.currentTimeMillis() < expiresAt

    private fun streamStillFresh(url: String): Boolean {
        val expire = expireSeconds(url) ?: return true
        return System.currentTimeMillis() + 90_000L < expire * 1000L
    }

    private fun expiresAtFor(url: String): Long {
        val now = System.currentTimeMillis()
        val fromUrl = expireSeconds(url)?.times(1000L)?.minus(4L * 60L * 1000L)
        val fallback = now + fallbackTtlMs
        val capped = now + maxTtlMs
        return when {
            fromUrl == null -> fallback
            fromUrl <= now -> now
            else -> minOf(fromUrl, capped)
        }
    }

    private fun expireSeconds(url: String): Long? {
        return Regex("(?:[?&])expire=(\\d+)").find(url)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    private fun cacheKey(track: Track): String {
        return track.id.trim().ifBlank { track.videoUrl.trim() }
    }

    private fun resolveWithInnerTube(track: Track, profile: ClientProfile): DirectStream {
        val endpoint = "https://www.youtube.com/youtubei/v1/player?key=$apiKey&prettyPrint=false"
        val body = buildPlayerBody(track.id, profile).toString().toByteArray(StandardCharsets.UTF_8)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 3_500
            readTimeout = 6_500
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Origin", if (profile.clientName == "WEB_REMIX") "https://music.youtube.com" else "https://www.youtube.com")
            setRequestProperty("Referer", if (profile.clientName == "WEB_EMBEDDED_PLAYER") "https://www.youtube.com/embed/${track.id}" else track.videoUrl)
            setRequestProperty("User-Agent", profile.userAgent)
            setRequestProperty("X-Youtube-Client-Name", profile.clientHeaderName)
            setRequestProperty("X-Youtube-Client-Version", profile.clientVersion)
            setRequestProperty("Content-Length", body.size.toString())
        }
        try {
            connection.outputStream.use { it.write(body) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.let { input -> BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader -> reader.readText() } }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            val root = JSONObject(response)
            val playability = root.optJSONObject("playabilityStatus")
            val status = playability?.optString("status").orEmpty()
            if (status.isNotBlank() && status != "OK") {
                val reason = playability?.optString("reason").orEmpty()
                val subreason = playability?.optJSONObject("errorScreen")?.toString().orEmpty()
                throw IllegalStateException(reason.ifBlank { subreason.ifBlank { status } })
            }
            val streamingData = root.optJSONObject("streamingData") ?: throw IllegalStateException("Nessun blocco streamingData")
            val formats = streamingData.optJSONArray("adaptiveFormats") ?: JSONArray()
            var bestUrl = ""
            var bestScore = -1
            for (i in 0 until formats.length()) {
                val format = formats.optJSONObject(i) ?: continue
                val mime = format.optString("mimeType")
                val url = format.optString("url")
                if (!mime.startsWith("audio/", true) || url.isBlank()) continue
                val bitrate = format.optInt("bitrate", 0)
                val audioQuality = format.optString("audioQuality")
                val mimeBoost = when {
                    mime.contains("mp4", true) -> 120_000
                    mime.contains("webm", true) -> 80_000
                    else -> 0
                }
                val score = bitrate + mimeBoost + when {
                    audioQuality.contains("HIGH", true) -> 900_000
                    audioQuality.contains("MEDIUM", true) -> 500_000
                    else -> 0
                }
                if (score > bestScore) {
                    bestScore = score
                    bestUrl = url
                }
            }
            if (bestUrl.isBlank()) throw IllegalStateException("Audio URL diretta assente")
            val details = root.optJSONObject("videoDetails")
            val duration = details?.optString("lengthSeconds")?.toLongOrNull()?.times(1000L) ?: 0L
            val thumbnail = details?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.bestThumbnail().orEmpty()
            return DirectStream(
                url = bestUrl,
                durationMs = duration,
                thumbnailUrl = thumbnail,
                source = "YouTube Music ${profile.label}"
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun resolveWithNewPipe(track: Track): Track {
        NewPipeRuntime.ensure()
        val info = StreamInfo.getInfo(ServiceList.YouTube, track.videoUrl)
        val audio = info.audioStreams
            .filter { it.isUrl && it.content.isNotBlank() }
            .maxWithOrNull(compareBy<AudioStream> { it.averageBitrate }.thenBy { it.formatId })
        val url = audio?.content
            ?: info.hlsUrl.takeIf { it.isNotBlank() }
            ?: info.videoStreams.firstOrNull { it.isUrl && it.content.isNotBlank() }?.content
            ?: throw IllegalStateException("Nessuno stream audio disponibile per ${track.title}")
        val bestThumb = info.thumbnails.maxByOrNull { image ->
            image.width.coerceAtLeast(0) * image.height.coerceAtLeast(0)
        }?.url.orEmpty()
        return track.copy(
            streamUrl = url,
            durationMs = if (info.duration > 0L) info.duration * 1000L else track.durationMs,
            thumbnailUrl = bestThumb.ifBlank { track.thumbnailUrl },
            largeThumbnailUrl = bestThumb.ifBlank { track.largeThumbnailUrl },
            source = "NewPipe YouTube"
        )
    }

    private fun buildPlayerBody(videoId: String, profile: ClientProfile): JSONObject {
        val client = JSONObject()
            .put("clientName", profile.clientName)
            .put("clientVersion", profile.clientVersion)
            .put("hl", "it")
            .put("gl", "IT")
        if (profile.android) {
            client.put("androidSdkVersion", 35)
                .put("osName", "Android")
                .put("osVersion", "15")
                .put("platform", "MOBILE")
        }
        if (profile.clientName == "IOS") {
            client.put("deviceMake", "Apple")
                .put("deviceModel", "iPhone16,2")
                .put("osName", "iPhone")
                .put("osVersion", "18.3")
                .put("platform", "MOBILE")
        }
        if (profile.clientName == "WEB_EMBEDDED_PLAYER") {
            client.put("clientScreen", "EMBED")
                .put("thirdParty", JSONObject().put("embedUrl", "https://www.youtube.com/embed/$videoId"))
        }
        return JSONObject()
            .put("context", JSONObject().put("client", client))
            .put("videoId", videoId)
            .put("contentCheckOk", true)
            .put("racyCheckOk", true)
            .put("playbackContext", JSONObject().put("contentPlaybackContext", JSONObject().put("html5Preference", "HTML5_PREF_WANTS")))
            .put("params", "CgIQBg")
            .put("watchEndpointMusicSupportedConfigs", JSONObject().put("watchEndpointMusicConfig", JSONObject().put("musicVideoType", "MUSIC_VIDEO_TYPE_ATV")))
    }

    private fun JSONArray.bestThumbnail(): String {
        var best = ""
        var score = -1
        for (i in 0 until length()) {
            val item = optJSONObject(i) ?: continue
            val url = item.optString("url")
            val current = item.optInt("width", 0) * item.optInt("height", 0)
            if (url.isNotBlank() && current >= score) {
                best = url
                score = current
            }
        }
        return best
    }
}

class PlaybackBlockedException(message: String) : IllegalStateException(message)

private data class ClientProfile(
    val clientName: String,
    val clientVersion: String,
    val label: String,
    val userAgent: String,
    val android: Boolean,
    val delayMs: Long
) {
    val clientHeaderName: String
        get() = when (clientName) {
            "ANDROID" -> "3"
            "ANDROID_MUSIC" -> "21"
            "IOS" -> "5"
            "WEB_REMIX" -> "67"
            "WEB_EMBEDDED_PLAYER" -> "56"
            else -> "1"
        }
}

private data class DirectStream(
    val url: String,
    val durationMs: Long,
    val thumbnailUrl: String,
    val source: String
)

private data class CachedStream(
    val track: Track,
    val expiresAt: Long
)
