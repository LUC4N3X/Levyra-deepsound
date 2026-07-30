package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.security.GoogleApiKeyHeaders
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

internal class YoutubeMusicChartsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val httpClient = LevyraHttpClientFactory.feeds(appContext)
    private val musicRepository = YoutubeMusicRepository(appContext)
    private val playlistStore = appContext.getSharedPreferences(PLAYLIST_STORE_NAME, Context.MODE_PRIVATE)

    suspend fun topTracks(country: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val request = chartRequest(country, limit)
        val cache = readPlaylistCache(request.country, System.currentTimeMillis())
        val candidates = resolvePlaylistCandidates(request, cache)
        val tracks = loadFirstAvailablePlaylist(candidates, request)
        if (tracks.isNotEmpty()) tracks else refreshFreshCache(request, cache)
    }

    private fun chartRequest(country: String, limit: Int): ChartRequest {
        return ChartRequest(
            country = country.trim().uppercase(Locale.ROOT).takeIf { it.length == 2 } ?: DEFAULT_COUNTRY,
            limit = limit.coerceIn(1, 100),
            languageCode = LevyraLanguageCatalog.deviceDefault()
        )
    }

    private fun readPlaylistCache(country: String, now: Long): PlaylistCache {
        val playlistId = playlistStore.getString(playlistKey(country), "").orEmpty()
        val updatedAt = playlistStore.getLong(playlistTimestampKey(country), 0L)
        val isFresh = playlistId.isNotBlank() && now - updatedAt in 0 until PLAYLIST_ID_TTL_MS
        return PlaylistCache(playlistId = playlistId, isFresh = isFresh)
    }

    private suspend fun resolvePlaylistCandidates(
        request: ChartRequest,
        cache: PlaylistCache
    ): List<String> {
        if (cache.isFresh) return listOf(cache.playlistId)
        val discovered = discoverAndPersistPlaylistId(request)
        return buildList {
            if (discovered.isNotBlank()) add(discovered)
            if (cache.playlistId.isNotBlank() && cache.playlistId != discovered) add(cache.playlistId)
        }
    }

    private suspend fun refreshFreshCache(
        request: ChartRequest,
        cache: PlaylistCache
    ): List<Track> {
        if (!cache.isFresh) return emptyList()
        val discovered = discoverAndPersistPlaylistId(request)
        if (discovered.isBlank() || discovered == cache.playlistId) return emptyList()
        return loadPlaylist(discovered, request)
    }

    private suspend fun discoverAndPersistPlaylistId(request: ChartRequest): String {
        val playlistId = discoverPlaylistId(request.country, request.languageCode)
        if (playlistId.isNotBlank()) {
            persistPlaylistId(request.country, playlistId, System.currentTimeMillis())
        }
        return playlistId
    }

    private suspend fun loadFirstAvailablePlaylist(
        candidates: List<String>,
        request: ChartRequest
    ): List<Track> {
        for (playlistId in candidates) {
            val tracks = loadPlaylist(playlistId, request)
            if (tracks.isNotEmpty()) return tracks
        }
        return emptyList()
    }

    private suspend fun loadPlaylist(
        playlistId: String,
        request: ChartRequest
    ): List<Track> {
        val tracks = try {
            fetchPlaylistTracks(playlistId, request)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Timber.w(error, "YouTube Music chart playlist failed for %s", playlistId)
            emptyList()
        }
        return tracks
            .asSequence()
            .filter { it.id.isNotBlank() && it.title.isNotBlank() && it.videoUrl.isNotBlank() }
            .distinctBy { it.id }
            .take(request.limit)
            .map { track ->
                track.copy(
                    album = track.album.ifBlank { CHART_ALBUM },
                    source = CHART_SOURCE,
                    moodTags = track.moodTags + setOf("chart", "hit"),
                    replayScore = maxOf(track.replayScore, 90),
                    cacheScore = maxOf(track.cacheScore, 85)
                )
            }
            .toList()
    }

    private suspend fun fetchPlaylistTracks(
        playlistId: String,
        request: ChartRequest
    ): List<Track> {
        val tracks = LinkedHashMap<String, Track>()
        val requestedContinuations = mutableSetOf<String>()
        var continuation = ""
        var page = 0
        while (tracks.size < request.limit && page < MAX_PLAYLIST_PAGES) {
            currentCoroutineContext().ensureActive()
            if (continuation.isNotBlank() && !requestedContinuations.add(continuation)) break
            val payload = playlistPayload(playlistId, continuation, request)
            val body = postBrowse(payload, "https://music.youtube.com/playlist?list=$playlistId") ?: break
            currentCoroutineContext().ensureActive()
            val root = runCatching { JSONObject(body) }.getOrNull() ?: break
            musicRepository.parsePlaylistTracks(root, playlistId).forEach { track ->
                tracks.putIfAbsent(track.id, track)
            }
            continuation = YoutubeMusicPlaylistPageParser.continuation(root)
            page += 1
            if (continuation.isBlank()) break
        }
        return tracks.values.take(request.limit)
    }

    private fun playlistPayload(
        playlistId: String,
        continuation: String,
        request: ChartRequest
    ): JSONObject {
        return JSONObject()
            .put("context", clientContext(request.languageCode, request.country))
            .apply {
                if (continuation.isBlank()) put("browseId", "VL$playlistId")
                else put("continuation", continuation)
            }
    }

    private suspend fun discoverPlaylistId(country: String, languageCode: String): String {
        val payload = JSONObject()
            .put("context", clientContext(languageCode, country))
            .put("browseId", "FEmusic_charts")
            .put(
                "formData",
                JSONObject().put("selectedValues", JSONArray().put(country))
            )
        val body = postBrowse(payload, "https://music.youtube.com/charts") ?: return ""
        return YoutubeMusicChartPageParser.firstPlaylistId(body)
    }

    private fun clientContext(languageCode: String, country: String): JSONObject {
        val locale = LevyraContentLocales.forLanguage(languageCode)
        return JSONObject().put(
            "client",
            JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", CLIENT_VERSION)
                .put("hl", locale.hl)
                .put("gl", country)
        )
    }

    private suspend fun postBrowse(payload: JSONObject, referer: String): String? =
        suspendCancellableCoroutine { continuation ->
            val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val builder = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse?key=${BuildConfig.YOUTUBE_INNERTUBE_API_KEY}&prettyPrint=false")
                .post(requestBody)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", referer)
                .header("User-Agent", USER_AGENT)
                .header("X-Youtube-Client-Name", "67")
                .header("X-Youtube-Client-Version", CLIENT_VERSION)
            val request = GoogleApiKeyHeaders.applyTo(builder, appContext).build()
            val call = httpClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching {
                        response.use { current ->
                            if (!current.isSuccessful) null else current.body.string().takeIf(String::isNotBlank)
                        }
                    }.getOrNull()
                    if (continuation.isActive) continuation.resume(result)
                }
            })
        }

    private fun persistPlaylistId(country: String, playlistId: String, timestamp: Long) {
        playlistStore.edit()
            .putString(playlistKey(country), playlistId)
            .putLong(playlistTimestampKey(country), timestamp)
            .apply()
    }

    private fun playlistKey(country: String): String = "playlist.$country"

    private fun playlistTimestampKey(country: String): String = "playlist.$country.updatedAt"

    private data class ChartRequest(
        val country: String,
        val limit: Int,
        val languageCode: String
    )

    private data class PlaylistCache(
        val playlistId: String,
        val isFresh: Boolean
    )

    private companion object {
        const val DEFAULT_COUNTRY = "IT"
        const val CLIENT_VERSION = "1.20260423.01.00"
        const val PLAYLIST_STORE_NAME = "levyra_youtube_music_charts"
        const val PLAYLIST_ID_TTL_MS = 7L * 24L * 60L * 60L * 1000L
        const val MAX_PLAYLIST_PAGES = 8
        const val CHART_SOURCE = "YouTube Music Charts"
        const val CHART_ALBUM = "YouTube Music Charts"
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

internal object YoutubeMusicChartPageParser {
    fun firstPlaylistId(body: String): String {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return ""
        return firstPlaylistId(root)
    }

    fun firstPlaylistId(root: JSONObject): String {
        val carousels = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicCarouselShelfRenderer", carousels)
        for (carousel in carousels) {
            val contents = carousel.optJSONArray("contents") ?: continue
            val playlistIds = ArrayList<String>(contents.length())
            for (index in 0 until contents.length()) {
                val playlistId = playlistBrowseId(contents.opt(index))
                if (playlistId.isNotBlank()) playlistIds += playlistId
            }
            if (playlistIds.isNotEmpty()) return playlistIds.first()
        }
        return ""
    }

    private fun playlistBrowseId(value: Any?): String {
        val endpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(value, "browseEndpoint", endpoints)
        return endpoints.firstNotNullOfOrNull { endpoint ->
            endpoint.optString("browseId")
                .trim()
                .takeIf { it.startsWith("VL") && it.length > 2 }
                ?.removePrefix("VL")
        }.orEmpty()
    }

    private fun collectObjectsByKey(value: Any?, targetKey: String, output: MutableList<JSONObject>) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val child = value.opt(key)
                    if (key == targetKey && child is JSONObject) output += child
                    collectObjectsByKey(child, targetKey, output)
                }
            }
            is JSONArray -> {
                for (index in 0 until value.length()) {
                    collectObjectsByKey(value.opt(index), targetKey, output)
                }
            }
        }
    }
}
