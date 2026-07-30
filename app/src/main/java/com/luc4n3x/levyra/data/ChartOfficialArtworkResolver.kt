package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Replaces cropped YouTube video frames with verified release artwork without changing chart order
 * or the native YouTube playback identity. Cached matches are instant; unresolved entries retain the
 * YouTube thumbnail only when every official provider fails or the strict chart artwork budget ends.
 */
internal class ChartOfficialArtworkResolver(context: Context) {
    private val appContext = context.applicationContext
    private val repositories = Array(PROVIDER_LANES) { OfficialArtworkRepository(appContext) }
    private val youtubeMusicRepository = YoutubeMusicRepository(appContext)
    private val store = appContext.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
    private val memory = ConcurrentHashMap<String, CachedArtwork>()
    private val keyLocks = Array(KEY_LOCK_COUNT) { Mutex() }
    private val lookupSlots = Semaphore(MAX_CONCURRENT_LOOKUPS)

    suspend fun enrich(tracks: List<Track>, country: String): List<Track> = withContext(Dispatchers.IO) {
        if (tracks.isEmpty()) return@withContext tracks
        val normalizedCountry = country.trim().uppercase(Locale.ROOT).takeIf { it.length == 2 } ?: DEFAULT_COUNTRY
        val resolved = ConcurrentHashMap<String, CachedArtwork>()

        withTimeoutOrNull(TOTAL_ARTWORK_BUDGET_MS) {
            coroutineScope {
                tracks.map { track ->
                    async {
                        val key = identityKey(track)
                        val artwork = lookupSlots.withPermit { resolve(key, track, normalizedCountry) }
                        if (artwork != null) resolved[key] = artwork
                    }
                }.awaitAll()
            }
        }

        tracks.map { track ->
            resolved[identityKey(track)]?.applyTo(track) ?: cached(identityKey(track))?.applyTo(track) ?: track
        }
    }

    private suspend fun resolve(key: String, track: Track, country: String): CachedArtwork? {
        cached(key)?.let { return it }
        if (hasFreshMiss(key, country)) return null
        val lock = keyLocks[(key.hashCode() and Int.MAX_VALUE) % keyLocks.size]
        return lock.withLock {
            cached(key)?.let { return@withLock it }
            if (hasFreshMiss(key, country)) return@withLock null
            val official = try {
                repositoryFor(key).find(track, country)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                null
            }
            val artwork = if (official != null) {
                CachedArtwork.from(official)
            } else {
                findYoutubeFallback(track)
            }
            if (artwork == null) {
                store.edit().putLong(missKey(key, country), System.currentTimeMillis()).apply()
                return@withLock null
            }
            memory[key] = artwork
            store.edit()
                .putString(artworkKey(key), artwork.toJson().toString())
                .remove(missKey(key, country))
                .apply()
            artwork
        }
    }

    private suspend fun findYoutubeFallback(track: Track): CachedArtwork? {
        val candidates = runCatching {
            youtubeMusicRepository.search("${track.title} ${track.artist}", 8)
        }.getOrDefault(emptyList())
        val best = candidates
            .asSequence()
            .map { candidate -> candidate to youtubeMatchScore(track, candidate) }
            .filter { (_, score) -> score >= MIN_YOUTUBE_MATCH_SCORE }
            .maxByOrNull { (_, score) -> score }
            ?.first
            ?: return null
        val prepared = LevyraPersonalOrbit.withoutVideoArtwork(best)
        val artwork = prepared.largeThumbnailUrl.trim()
            .ifBlank { prepared.thumbnailUrl.trim() }
            .ifBlank { best.largeThumbnailUrl.trim() }
            .ifBlank { best.thumbnailUrl.trim() }
        if (artwork.isBlank()) return null
        return CachedArtwork.fromYoutube(prepared.copy(thumbnailUrl = artwork, largeThumbnailUrl = artwork))
    }

    private fun youtubeMatchScore(target: Track, candidate: Track): Int {
        val targetTitle = ChartFeedParser.normalizeMusicText(target.title)
        val targetArtist = ChartFeedParser.normalizeMusicText(target.artist)
        val candidateTitle = ChartFeedParser.normalizeMusicText(candidate.title)
        val candidateArtist = ChartFeedParser.normalizeMusicText(candidate.artist)
        var score = when {
            candidateTitle == targetTitle -> 140
            candidateTitle.contains(targetTitle) || targetTitle.contains(candidateTitle) -> 90
            else -> 0
        }
        score += when {
            candidateArtist == targetArtist -> 90
            candidateArtist.contains(targetArtist) || targetArtist.contains(candidateArtist) -> 52
            else -> 0
        }
        if (target.durationMs > 0L && candidate.durationMs > 0L) {
            val delta = kotlin.math.abs(target.durationMs - candidate.durationMs)
            if (delta <= 6_000L) score += 30 else if (delta > 40_000L) score -= 30
        }
        val searchable = "${candidate.title} ${candidate.artist} ${candidate.album}".lowercase()
        if (listOf("karaoke", "cover", "reaction", "nightcore", "sped up", "slowed").any(searchable::contains)) score -= 70
        return score
    }

    private fun repositoryFor(key: String): OfficialArtworkRepository {
        return repositories[(key.hashCode() and Int.MAX_VALUE) % repositories.size]
    }

    private fun cached(key: String): CachedArtwork? {
        memory[key]?.let { cached ->
            if (cached.isFresh()) return cached
            memory.remove(key, cached)
        }
        val raw = store.getString(artworkKey(key), null) ?: return null
        val cached = runCatching { CachedArtwork.from(JSONObject(raw)) }.getOrNull()
        if (cached == null || !cached.isFresh()) {
            store.edit().remove(artworkKey(key)).apply()
            return null
        }
        memory[key] = cached
        return cached
    }

    private fun hasFreshMiss(key: String, country: String): Boolean {
        val recordedAt = store.getLong(missKey(key, country), 0L)
        if (recordedAt <= 0L) return false
        val fresh = System.currentTimeMillis() - recordedAt in 0 until MISS_TTL_MS
        if (!fresh) store.edit().remove(missKey(key, country)).apply()
        return fresh
    }

    private fun identityKey(track: Track): String {
        val normalized = listOf(
            ChartFeedParser.normalizeMusicText(track.title),
            ChartFeedParser.normalizeMusicText(track.artist)
        ).joinToString("|")
        return MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
            .take(12)
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun artworkKey(key: String): String = "artwork.$key"

    private fun missKey(key: String, country: String): String = "miss.$country.$key"

    private data class CachedArtwork(
        val thumbnailUrl: String,
        val largeThumbnailUrl: String,
        val album: String,
        val provider: String,
        val canonicalAlbumUrl: String,
        val releaseDate: String,
        val year: String,
        val trackNumber: Int,
        val discNumber: Int,
        val explicit: Boolean,
        val isrc: String,
        val upc: String,
        val score: Int,
        val cachedAt: Long
    ) {
        fun isFresh(now: Long = System.currentTimeMillis()): Boolean =
            thumbnailUrl.isNotBlank() && now - cachedAt in 0 until ARTWORK_TTL_MS

        fun applyTo(track: Track): Track {
            return track.copy(
                album = album.ifBlank { track.album },
                thumbnailUrl = thumbnailUrl,
                largeThumbnailUrl = largeThumbnailUrl.ifBlank { thumbnailUrl },
                isrc = isrc.ifBlank { track.isrc },
                upc = upc.ifBlank { track.upc },
                releaseDate = releaseDate.ifBlank { track.releaseDate },
                year = year.ifBlank { track.year },
                trackNumber = trackNumber.takeIf { it > 0 } ?: track.trackNumber,
                discNumber = discNumber.takeIf { it > 0 } ?: track.discNumber,
                explicit = explicit || track.explicit,
                metadataProvider = provider.ifBlank { track.metadataProvider },
                metadataConfidence = maxOf(track.metadataConfidence, confidence(score)),
                canonicalAlbumUrl = canonicalAlbumUrl.ifBlank { track.canonicalAlbumUrl },
                moodTags = track.moodTags + EDITORIAL_ARTWORK_LOCK_TAG
            )
        }

        fun toJson(): JSONObject = JSONObject()
            .put("thumbnailUrl", thumbnailUrl)
            .put("largeThumbnailUrl", largeThumbnailUrl)
            .put("album", album)
            .put("provider", provider)
            .put("canonicalAlbumUrl", canonicalAlbumUrl)
            .put("releaseDate", releaseDate)
            .put("year", year)
            .put("trackNumber", trackNumber)
            .put("discNumber", discNumber)
            .put("explicit", explicit)
            .put("isrc", isrc)
            .put("upc", upc)
            .put("score", score)
            .put("cachedAt", cachedAt)

        companion object {
            fun from(value: OfficialArtworkRepository.OfficialArtwork): CachedArtwork = CachedArtwork(
                thumbnailUrl = value.thumbnailUrl,
                largeThumbnailUrl = value.largeThumbnailUrl,
                album = value.album,
                provider = value.provider,
                canonicalAlbumUrl = value.canonicalAlbumUrl,
                releaseDate = value.releaseDate,
                year = value.year,
                trackNumber = value.trackNumber,
                discNumber = value.discNumber,
                explicit = value.explicit,
                isrc = value.isrc,
                upc = value.upc,
                score = value.score,
                cachedAt = System.currentTimeMillis()
            )

            fun fromYoutube(value: Track): CachedArtwork = CachedArtwork(
                thumbnailUrl = value.thumbnailUrl,
                largeThumbnailUrl = value.largeThumbnailUrl.ifBlank { value.thumbnailUrl },
                album = value.album,
                provider = value.metadataProvider.ifBlank { value.source.ifBlank { "YouTube Music" } },
                canonicalAlbumUrl = value.canonicalAlbumUrl,
                releaseDate = value.releaseDate,
                year = value.year,
                trackNumber = value.trackNumber,
                discNumber = value.discNumber,
                explicit = value.explicit,
                isrc = value.isrc,
                upc = value.upc,
                score = MIN_YOUTUBE_MATCH_SCORE,
                cachedAt = System.currentTimeMillis()
            )

            fun from(json: JSONObject): CachedArtwork = CachedArtwork(
                thumbnailUrl = json.optString("thumbnailUrl"),
                largeThumbnailUrl = json.optString("largeThumbnailUrl"),
                album = json.optString("album"),
                provider = json.optString("provider"),
                canonicalAlbumUrl = json.optString("canonicalAlbumUrl"),
                releaseDate = json.optString("releaseDate"),
                year = json.optString("year"),
                trackNumber = json.optInt("trackNumber", 0),
                discNumber = json.optInt("discNumber", 0),
                explicit = json.optBoolean("explicit", false),
                isrc = json.optString("isrc"),
                upc = json.optString("upc"),
                score = json.optInt("score", 0),
                cachedAt = json.optLong("cachedAt", 0L)
            )

            private fun confidence(score: Int): Int = when {
                score >= 500 -> 100
                score >= 420 -> 96
                score >= 360 -> 91
                score >= 320 -> 86
                score >= 280 -> 80
                score >= 240 -> 72
                score >= 200 -> 64
                else -> (score / 4).coerceIn(0, 63)
            }
        }
    }

    private companion object {
        const val CACHE_NAME = "levyra_chart_official_artwork"
        const val DEFAULT_COUNTRY = "IT"
        const val PROVIDER_LANES = 2
        const val KEY_LOCK_COUNT = 64
        const val MAX_CONCURRENT_LOOKUPS = 8
        const val TOTAL_ARTWORK_BUDGET_MS = 7_000L
        const val ARTWORK_TTL_MS = 90L * 24L * 60L * 60L * 1000L
        const val MISS_TTL_MS = 12L * 60L * 60L * 1000L
        const val MIN_YOUTUBE_MATCH_SCORE = 150
    }
}
