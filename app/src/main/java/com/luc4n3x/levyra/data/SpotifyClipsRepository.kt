package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

internal const val SPOTIFY_CLIP_ID_PREFIX = "spotify-clip-"
private const val CLIPS_ASSET = "editorial/spotify-clips.json"
private const val CLIPS_REMOTE_URL =
    "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/editorial-data/catalog/spotify-clips.json"
private const val CLIPS_REMOTE_TIMEOUT_MS = 6_000L
private const val CLIPS_REMOTE_TTL_MS = 6 * 60 * 60 * 1_000L
private val CANVAS_URL_REGEX = Regex("^https://canvaz\\.scdn\\.co/[A-Za-z0-9/_.\\-]+\\.cnvs\\.mp4$")
private val ARTWORK_URL_REGEX = Regex("^https://i\\.scdn\\.co/image/[A-Za-z0-9]+$")
private val VIDEO_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")

internal data class SpotifyClip(
    val id: String,
    val genre: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val canvasUrl: String,
    val audioVideoId: String
)

internal data class SpotifyClipsCatalog(
    val genreTitles: Map<String, String>,
    val genreLanguages: Map<String, Set<String>>,
    val clips: List<SpotifyClip>
)

internal object SpotifyClipCanvasRegistry {
    private val canvasByTrackId = ConcurrentHashMap<String, String>()
    private val titleByTrackId = ConcurrentHashMap<String, String>()
    @Volatile private var bundledSource: Context? = null
    @Volatile private var bundledLoaded = false

    fun attach(context: Context) {
        bundledSource = context.applicationContext
    }

    fun register(trackId: String, canvasUrl: String, feedTitle: String) {
        canvasByTrackId[trackId] = canvasUrl
        if (feedTitle.isNotBlank()) titleByTrackId[trackId] = feedTitle
    }

    fun feedTitle(track: Track?): String {
        if (canvasUrl(track).isBlank()) return ""
        return track?.let { titleByTrackId[it.id] }.orEmpty()
    }

    fun canvasUrl(track: Track?): String {
        val id = track?.id ?: return ""
        canvasByTrackId[id]?.let { return it }
        if (!id.startsWith(SPOTIFY_CLIP_ID_PREFIX) || bundledLoaded) return ""
        val context = bundledSource ?: return ""
        bundledLoaded = true
        runCatching {
            context.assets.open(CLIPS_ASSET).bufferedReader().use { parseSpotifyClips(it.readText()) }
        }.getOrNull()?.let { catalog ->
            catalog.clips.forEach { clip ->
                canvasByTrackId.putIfAbsent(SPOTIFY_CLIP_ID_PREFIX + clip.id, clip.canvasUrl)
                catalog.genreTitles[clip.genre]?.let { titleByTrackId.putIfAbsent(SPOTIFY_CLIP_ID_PREFIX + clip.id, it) }
            }
        }
        return canvasByTrackId[id].orEmpty()
    }
}

internal class SpotifyClipsRepository(private val context: Context) {
    @Volatile private var cached: SpotifyClipsCatalog? = null

    init {
        SpotifyClipCanvasRegistry.attach(context)
    }
    @Volatile private var remoteCheckedAtMs = 0L

    suspend fun feed(languageCode: String, preferredArtists: List<String>, limit: Int): List<Track> {
        val catalog = load() ?: return emptyList()
        return rankSpotifyClips(catalog, languageCode, preferredArtists, limit, LocalDate.now().toEpochDay())
            .map { clip -> clip.toTrack(catalog.genreTitles[clip.genre].orEmpty()) }
    }

    private suspend fun load(): SpotifyClipsCatalog? {
        val now = System.currentTimeMillis()
        if (now - remoteCheckedAtMs > CLIPS_REMOTE_TTL_MS) {
            remoteCheckedAtMs = now
            remote()?.let { cached = it }
        }
        cached?.let { return it }
        return bundled()?.also { cached = it }
    }

    private suspend fun remote(): SpotifyClipsCatalog? = try {
        withTimeoutOrNull(CLIPS_REMOTE_TIMEOUT_MS) {
            runInterruptible {
                val request = Request.Builder().url(CLIPS_REMOTE_URL).build()
                LevyraHttpClientFactory.general(context).newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful && body.isNotBlank()) parseSpotifyClips(body) else null
                }
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Timber.w(error, "Spotify clips refresh failed")
        null
    }

    private fun bundled(): SpotifyClipsCatalog? = try {
        context.assets.open(CLIPS_ASSET).bufferedReader().use { parseSpotifyClips(it.readText()) }
    } catch (error: Exception) {
        Timber.w(error, "Bundled Spotify clips unavailable")
        null
    }
}

internal fun parseSpotifyClips(raw: String): SpotifyClipsCatalog? {
    val root = JSONObject(raw)
    val genres = root.optJSONArray("genres") ?: return null
    val titles = buildMap {
        for (index in 0 until genres.length()) {
            val genre = genres.optJSONObject(index) ?: continue
            put(genre.optString("id"), genre.optString("title").trim())
        }
    }
    val languages = buildMap {
        for (index in 0 until genres.length()) {
            val genre = genres.optJSONObject(index) ?: continue
            val codes = genre.optJSONArray("languages")
            put(
                genre.optString("id"),
                buildSet { for (code in 0 until (codes?.length() ?: 0)) add(codes!!.optString(code).lowercase(Locale.ROOT)) }
            )
        }
    }
    val items = root.optJSONArray("items") ?: return null
    val clips = buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val canvas = item.optString("canvasUrl")
            val title = item.optString("title").trim()
            val artist = item.optString("artist").trim()
            if (!CANVAS_URL_REGEX.matches(canvas) || title.isBlank() || artist.isBlank()) continue
            add(
                SpotifyClip(
                    id = item.optString("id").ifBlank { canvas.hashCode().toString() },
                    genre = item.optString("genre"),
                    title = title,
                    artist = artist,
                    artworkUrl = item.optString("artworkUrl").takeIf(ARTWORK_URL_REGEX::matches).orEmpty(),
                    canvasUrl = canvas,
                    audioVideoId = item.optString("audioVideoId").takeIf(VIDEO_ID_REGEX::matches).orEmpty()
                )
            )
        }
    }
    return SpotifyClipsCatalog(titles, languages, clips).takeIf { clips.isNotEmpty() }
}

internal fun rankSpotifyClips(
    catalog: SpotifyClipsCatalog,
    languageCode: String,
    preferredArtists: List<String>,
    limit: Int,
    daySeed: Long
): List<SpotifyClip> {
    if (limit <= 0) return emptyList()
    val language = languageCode.substringBefore('-').lowercase(Locale.ROOT)
    val preferred = preferredArtists.flatMap(::clipArtistKeys).toSet()
    fun affinity(clip: SpotifyClip) = if (clipArtistKeys(clip.artist).any(preferred::contains)) 1 else 0
    val random = Random(daySeed)
    val byGenre = catalog.clips.groupBy { it.genre }.mapValues { (_, clips) ->
        clips.shuffled(random).sortedByDescending(::affinity).toMutableList()
    }
    val genreScores = byGenre.mapValues { (genre, genreClips) ->
        val languageScore = if (language in catalog.genreLanguages[genre].orEmpty()) 6 else 0
        languageScore + genreClips.sumOf(::affinity) * 2 + random.nextInt(3)
    }
    val genreOrder = byGenre.keys.sortedByDescending { genreScores.getValue(it) }
    val weights = genreOrder.mapIndexed { index, genre -> genre to if (index < 3) 2 else 1 }
    val result = mutableListOf<SpotifyClip>()
    while (result.size < limit && byGenre.values.any { it.isNotEmpty() }) {
        for ((genre, weight) in weights) {
            repeat(weight) {
                byGenre.getValue(genre).removeFirstOrNull()?.let(result::add)
            }
        }
    }
    return result.take(limit)
}

private fun clipArtistKeys(value: String): List<String> =
    value.split(',', '&').map { it.trim().lowercase(Locale.ROOT) }.filter { it.length >= 2 }

private fun SpotifyClip.toTrack(feedTitle: String): Track {
    val trackId = SPOTIFY_CLIP_ID_PREFIX + id
    SpotifyClipCanvasRegistry.register(trackId, canvasUrl, feedTitle)
    val palette = arrayOf(
        0xFF1ED760.toInt() to 0xFF0B5C2A.toInt(),
        0xFFE91429.toInt() to 0xFF5C0B12.toInt(),
        0xFF8D67AB.toInt() to 0xFF2D1B45.toInt(),
        0xFF1E3264.toInt() to 0xFF0D1730.toInt()
    )[(id.hashCode() and Int.MAX_VALUE) % 4]
    return Track(
        id = trackId,
        title = title,
        artist = artist,
        album = "",
        durationMs = 0L,
        streamUrl = "",
        videoUrl = audioVideoId.takeIf(String::isNotBlank)?.let { "https://www.youtube.com/watch?v=$it" }.orEmpty(),
        thumbnailUrl = artworkUrl,
        largeThumbnailUrl = artworkUrl,
        source = YOUTUBE_SHORTS_SOURCE,
        moodTags = setOf("shorts", genre),
        energy = 74,
        vocal = 60,
        replayScore = 86,
        cacheScore = 70,
        accentStart = palette.first,
        accentEnd = palette.second,
        videoType = "SHORTS"
    )
}
