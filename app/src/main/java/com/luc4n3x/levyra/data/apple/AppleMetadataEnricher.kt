package com.luc4n3x.levyra.data.apple

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.TimeUnit

class AppleMetadataEnricher(private val context: Context) {
    private val client: OkHttpClient = LevyraHttpClientFactory.general(context.applicationContext)
        .newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val tokenProvider = AppleDeveloperTokenProvider.get(context)
    private val cacheGuard = Any()
    private val cache = object : LinkedHashMap<String, AppleTrackMetadata>(CACHE_MAX_ENTRIES + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AppleTrackMetadata>): Boolean =
            size > CACHE_MAX_ENTRIES
    }
    private val misses = object : LinkedHashMap<String, Long>(MISS_CACHE_MAX_ENTRIES + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean =
            size > MISS_CACHE_MAX_ENTRIES
    }
    private var missSweepCounter = 0
    private val keyLocks = Array(32) { Mutex() }
    private val searchSlots = Semaphore(4)

    suspend fun enrich(track: Track, country: String? = null): Track? {
        if (track.title.isBlank() || track.artist.isBlank()) return null
        val targetCountry = country?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.length == 2 }
            ?: Locale.getDefault().country.takeIf { it.length == 2 }
            ?: "IT"

        val key = cacheKey(track, targetCountry)
        readCache(key)?.let { return mergeWithAppleMetadata(track, it) }

        val now = System.currentTimeMillis()
        if (hasFreshMiss(key, now)) return null

        val lock = keyLocks[(key.hashCode() and Int.MAX_VALUE) % keyLocks.size]
        return lock.withLock {
            readCache(key)?.let { return@withLock mergeWithAppleMetadata(track, it) }
            if (hasFreshMiss(key, System.currentTimeMillis())) return@withLock null

            val outcome = searchSlots.withPermit { searchAndEnrich(track, targetCountry) }
            writeOutcome(key, outcome)
            outcome?.let { mergeWithAppleMetadata(track, it) }
        }
    }

    private suspend fun searchAndEnrich(track: Track, country: String): AppleTrackMetadata? = withContext(Dispatchers.IO) {
        try {
            val candidates = searchAppleCatalog(track, country)
            if (candidates.isEmpty()) return@withContext null

            val evaluated = candidates.map { candidate ->
                val evaluation = AppleMetadataMatcher.evaluate(track, candidate)
                candidate.copy(confidence = evaluation.confidence) to evaluation
            }

            val best = evaluated
                .filter { it.second.accepted }
                .maxByOrNull { it.first.confidence }
                ?.first ?: return@withContext null

            supplementMetadata(best, country)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Apple metadata enrichment failed for %s", track.title)
            null
        }
    }

    private suspend fun searchAppleCatalog(track: Track, country: String): List<AppleTrackMetadata> {
        val query = buildSearchQuery(track)
        val url = APPLE_SEARCH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("term", query)
            .addQueryParameter("media", "music")
            .addQueryParameter("entity", "song")
            .addQueryParameter("limit", "15")
            .addQueryParameter("country", country)
            .addQueryParameter("explicit", "Yes")
            .build()

        val json = requestJson(url.toString()) ?: return emptyList()
        val results = json.optJSONArray("results") ?: return emptyList()
        val items = ArrayList<AppleTrackMetadata>(results.length())

        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val artworkUrl = item.optString("artworkUrl100").trim()
            val trackName = item.optString("trackName").trim()
            val artistName = item.optString("artistName").trim()
            if (trackName.isBlank() || artistName.isBlank()) continue

            val releaseDate = item.optString("releaseDate").trim()
            val trackId = item.optLong("trackId", 0L).takeIf { it > 0L }?.toString().orEmpty()
            val collectionId = item.optLong("collectionId", 0L).takeIf { it > 0L }?.toString().orEmpty()
            val durationMs = item.optLong("trackTimeMillis", 0L)
            val genre = item.optString("primaryGenreName").trim()

            items += AppleTrackMetadata(
                songId = trackId,
                albumId = collectionId,
                name = trackName,
                artistName = artistName,
                albumName = item.optString("collectionName").trim(),
                albumArtistName = item.optString("collectionArtistName").trim(),
                genreNames = if (genre.isNotBlank()) listOf(genre) else emptyList(),
                releaseDate = releaseDate,
                trackNumber = item.optInt("trackNumber", 0),
                trackTotal = item.optInt("trackCount", 0),
                discNumber = item.optInt("discNumber", 0),
                discTotal = item.optInt("discCount", 0),
                isrc = item.optString("isrc").trim(),
                explicit = item.optString("trackExplicitness").equals("explicit", ignoreCase = true),
                canonicalSongUrl = item.optString("trackViewUrl").trim(),
                canonicalAlbumUrl = item.optString("collectionViewUrl").trim(),
                artworkUrl = resizeAppleArtwork(artworkUrl, 600),
                highResArtworkUrl = resizeAppleArtwork(artworkUrl, 1400),
                durationMs = durationMs
            )
        }
        return items
    }

    private suspend fun supplementMetadata(base: AppleTrackMetadata, country: String): AppleTrackMetadata {
        val storefront = country.lowercase(Locale.ROOT)
        val devToken = tokenProvider.getDeveloperToken()
        if (devToken != null && base.songId.isNotBlank()) {
            val ampResult = fetchAmpSongDetail(base.songId, storefront, devToken)
            if (ampResult != null) {
                return mergeAmpMetadata(base, ampResult)
            }
        }

        if (base.albumId.isNotBlank()) {
            val itunesAlbum = fetchItunesAlbumLookup(base.albumId, country)
            if (itunesAlbum != null) {
                return base.copy(
                    copyright = itunesAlbum.optString("copyright").trim().ifBlank { base.copyright },
                    albumReleaseDate = itunesAlbum.optString("releaseDate").trim().ifBlank { base.albumReleaseDate },
                    trackTotal = base.trackTotal.takeIf { it > 0 } ?: itunesAlbum.optInt("trackCount", 0),
                    albumArtistName = base.albumArtistName.ifBlank { itunesAlbum.optString("artistName").trim() }
                )
            }
        }

        return base
    }

    private suspend fun fetchAmpSongDetail(songId: String, storefront: String, token: String): JSONObject? {
        val url = "$AMP_BASE_URL/v1/catalog/$storefront/songs/$songId?include=albums"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Origin", "https://music.apple.com")
            .header("Accept", "application/json")
            .build()
        return executeJson(request)
    }

    private fun mergeAmpMetadata(base: AppleTrackMetadata, ampRoot: JSONObject): AppleTrackMetadata {
        val songData = ampRoot.optJSONArray("data")?.optJSONObject(0) ?: return base
        val attr = songData.optJSONObject("attributes") ?: return base
        val relationships = songData.optJSONObject("relationships")
        val albumData = relationships?.optJSONObject("albums")?.optJSONArray("data")?.optJSONObject(0)
        val albumAttr = albumData?.optJSONObject("attributes")

        val composer = attr.optString("composerName").trim()
        val genres = attr.optJSONArray("genreNames")?.toStringList() ?: base.genreNames
        val albumArtist = attr.optString("albumArtistName").trim()
            .ifBlank { albumAttr?.optString("artistName").orEmpty().trim() }
        val copyright = albumAttr?.optString("copyright").orEmpty().trim()
        val upc = albumAttr?.optString("upc").orEmpty().trim()
        val albumReleaseDate = albumAttr?.optString("releaseDate").orEmpty().trim()
        val albumTrackCount = albumAttr?.optInt("trackCount", 0) ?: 0

        val rawArtworkUrl = attr.optJSONObject("artwork")?.optString("url").orEmpty().trim()
        val highResArtwork = if (rawArtworkUrl.isNotBlank()) {
            resolveArtworkUrl(rawArtworkUrl, 1400, 1400)
        } else {
            base.highResArtworkUrl
        }

        return base.copy(
            composerName = composer.ifBlank { base.composerName },
            genreNames = if (genres.isNotEmpty()) genres else base.genreNames,
            albumArtistName = albumArtist.ifBlank { base.albumArtistName },
            copyright = copyright.ifBlank { base.copyright },
            upc = upc.ifBlank { base.upc },
            albumReleaseDate = albumReleaseDate.ifBlank { base.albumReleaseDate },
            trackTotal = base.trackTotal.takeIf { it > 0 } ?: albumTrackCount,
            highResArtworkUrl = highResArtwork.ifBlank { base.highResArtworkUrl }
        )
    }

    private suspend fun fetchItunesAlbumLookup(albumId: String, country: String): JSONObject? {
        val url = APPLE_LOOKUP_URL.toHttpUrl().newBuilder()
            .addQueryParameter("id", albumId)
            .addQueryParameter("country", country)
            .build()
        val json = requestJson(url.toString()) ?: return null
        return json.optJSONArray("results")?.optJSONObject(0)
    }

    fun mergeWithAppleMetadata(track: Track, apple: AppleTrackMetadata): Track =
        Companion.mergeWithAppleMetadata(track, apple)

    private fun buildSearchQuery(track: Track): String {
        val cleanTitle = track.title
            .replace(Regex("\\([^)]*(official\\s*(video|audio)|lyrics?|visualizer|music\\s*video)[^)]*\\)", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\[[^]]*(official\\s*(video|audio)|lyrics?|visualizer|music\\s*video)[^]]*]", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+-\\s+(official\\s*(video|audio)|lyrics?|visualizer|music\\s*video).*$", RegexOption.IGNORE_CASE), " ")
            .trim()
        val cleanArtist = track.artist.trim()
        return "$cleanTitle $cleanArtist".trim()
    }

    private suspend fun requestJson(url: String): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()
        return executeJson(request)
    }

    private suspend fun executeJson(request: Request): JSONObject? = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else {
                    response.body.string().let { JSONObject(it) }
                }
            }
        } catch (_: IOException) {
            null
        }
    }

    private fun resizeAppleArtwork(url: String, size: Int): String =
        url.replace(APPLE_ARTWORK_SIZE, "${size}x${size}bb")

    private fun resolveArtworkUrl(rawUrl: String, width: Int, height: Int): String =
        rawUrl.replace("{w}", width.toString())
            .replace("{h}", height.toString())
            .replace("{c}", "bb")
            .replace("{f}", "jpg")

    private fun cacheKey(track: Track, country: String): String = listOf(
        track.title.trim().lowercase(Locale.ROOT),
        track.artist.trim().lowercase(Locale.ROOT),
        track.album.trim().lowercase(Locale.ROOT),
        track.isrc.trim().lowercase(Locale.ROOT),
        (track.durationMs / 5_000L).toString(),
        country.uppercase(Locale.ROOT)
    ).joinToString("|")

    private fun readCache(key: String): AppleTrackMetadata? = synchronized(cacheGuard) { cache[key] }

    private fun hasFreshMiss(key: String, now: Long): Boolean = synchronized(cacheGuard) {
        missSweepCounter++
        if (missSweepCounter >= MISS_SWEEP_INTERVAL) {
            misses.entries.removeAll { now - it.value >= MISS_TTL_MS }
            missSweepCounter = 0
        }
        val timestamp = misses[key] ?: return@synchronized false
        if (now - timestamp < MISS_TTL_MS) true else {
            misses.remove(key)
            false
        }
    }

    private fun writeOutcome(key: String, outcome: AppleTrackMetadata?) {
        synchronized(cacheGuard) {
            if (outcome != null) {
                cache[key] = outcome
                misses.remove(key)
            } else {
                misses[key] = System.currentTimeMillis()
            }
        }
    }

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (i in 0 until length()) {
            optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
        }
    }

    companion object {
        private const val APPLE_SEARCH_URL = "https://itunes.apple.com/search"
        private const val APPLE_LOOKUP_URL = "https://itunes.apple.com/lookup"
        private const val AMP_BASE_URL = "https://amp-api.music.apple.com"
        private const val USER_AGENT = "Levyra/2.3.20 Android"
        private const val CACHE_MAX_ENTRIES = 384
        private const val MISS_CACHE_MAX_ENTRIES = 512
        private const val MISS_SWEEP_INTERVAL = 64
        private const val MISS_TTL_MS = 10 * 60 * 1000L
        private val APPLE_ARTWORK_SIZE = Regex("\\d+x\\d+bb")

        fun mergeWithAppleMetadata(track: Track, apple: AppleTrackMetadata): Track {
            val parsedYear = apple.releaseDate.take(4).takeIf { it.all(Char::isDigit) } ?: track.year
            val resolvedAlbumArtist = apple.albumArtistName.ifBlank { track.albumArtist }.ifBlank { apple.artistName }

            val enrichedGenres = apple.genreNames.map { it.lowercase(Locale.ROOT) }.toSet()
            val combinedMoodTags = (track.moodTags + enrichedGenres).filter { it.isNotBlank() }.toSet()

            return track.copy(
                album = apple.albumName.ifBlank { track.album },
                albumArtist = resolvedAlbumArtist,
                composer = apple.composerName.ifBlank { track.composer },
                trackNumber = apple.trackNumber.takeIf { it > 0 } ?: track.trackNumber,
                trackTotal = apple.trackTotal.takeIf { it > 0 } ?: track.trackTotal,
                discNumber = apple.discNumber.takeIf { it > 0 } ?: track.discNumber,
                discTotal = apple.discTotal.takeIf { it > 0 } ?: track.discTotal,
                isrc = apple.isrc.ifBlank { track.isrc },
                upc = apple.upc.ifBlank { track.upc },
                releaseDate = apple.releaseDate.ifBlank { track.releaseDate },
                year = parsedYear.ifBlank { track.year },
                copyright = apple.copyright.ifBlank { track.copyright },
                explicit = apple.explicit || track.explicit,
                thumbnailUrl = apple.artworkUrl.ifBlank { track.thumbnailUrl },
                largeThumbnailUrl = apple.highResArtworkUrl.ifBlank { track.largeThumbnailUrl },
                canonicalAlbumUrl = apple.canonicalAlbumUrl.ifBlank { track.canonicalAlbumUrl },
                appleSongId = apple.songId.ifBlank { track.appleSongId },
                appleAlbumId = apple.albumId.ifBlank { track.appleAlbumId },
                metadataProvider = "Apple Music",
                metadataConfidence = maxOf(track.metadataConfidence, apple.confidence),
                moodTags = combinedMoodTags
            )
        }
    }
}
