from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
YOUTUBE = ROOT / "app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicRepository.kt"
VIEWMODEL = ROOT / "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"
DESCRIPTION = ROOT / "app/src/main/java/com/luc4n3x/levyra/data/AlbumDescriptionRepository.kt"


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected one match in {path}: found {count}\n{old[:160]}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_regex_once(path: Path, pattern: str, replacement: str) -> None:
    text = path.read_text(encoding="utf-8")
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.DOTALL)
    if count != 1:
        raise SystemExit(f"Expected one regex match in {path}: found {count}\n{pattern[:160]}")
    path.write_text(updated, encoding="utf-8")


DESCRIPTION_CONTENT = r'''package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resolves album copy without making the album screen depend on one provider.
 *
 * YouTube Music remains the first source. When it has no description we try a
 * verified Wikipedia article, then Wikidata's CC0 entity description. Spotify
 * is used only when an actual open.spotify.com album URL is already present;
 * generic "Listen on Spotify" marketing copy is deliberately rejected.
 */
internal class AlbumDescriptionRepository(context: Context?) {
    private val client = LevyraHttpClientFactory.media(context?.applicationContext).newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private val cache = ConcurrentHashMap<String, String>()

    suspend fun resolve(
        album: AlbumHit,
        languageCode: String,
        youtubeDescription: String,
        trackCount: Int
    ): String {
        val native = cleanDescription(youtubeDescription)
        if (native.length >= MIN_DESCRIPTION_LENGTH) return native

        val language = LevyraLanguageCatalog.normalize(languageCode)
        val key = "${descriptionKey(album.title)}|${descriptionKey(album.artist)}|$language"
        cache[key]?.let { return it }

        val remote = withTimeoutOrNull(REMOTE_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                wikipediaDescription(album, language)
                    ?: wikidataDescription(album, language)
                    ?: spotifyDescription(album.canonicalUrl)
            }
        }
        val resolved = cleanDescription(remote.orEmpty())
            .takeIf { it.length >= MIN_DESCRIPTION_LENGTH }
            ?: factualFallback(album, language, trackCount)
        cache[key] = resolved
        return resolved
    }

    private fun wikipediaDescription(album: AlbumHit, languageCode: String): String? {
        val languages = linkedSetOf(languageCode, "en")
        for (language in languages) {
            val url = "https://$language.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("generator", "search")
                .addQueryParameter("gsrsearch", "\"${album.title}\" \"${album.artist}\" album")
                .addQueryParameter("gsrnamespace", "0")
                .addQueryParameter("gsrlimit", "6")
                .addQueryParameter("prop", "extracts|pageprops")
                .addQueryParameter("exintro", "1")
                .addQueryParameter("explaintext", "1")
                .addQueryParameter("exsentences", "3")
                .addQueryParameter("redirects", "1")
                .addQueryParameter("format", "json")
                .addQueryParameter("formatversion", "2")
                .addQueryParameter("origin", "*")
                .build()
            val pages = requestJson(url)
                ?.optJSONObject("query")
                ?.optJSONArray("pages")
                ?: continue
            var bestText: String? = null
            var bestScore = Int.MIN_VALUE
            for (index in 0 until pages.length()) {
                val page = pages.optJSONObject(index) ?: continue
                if (page.optJSONObject("pageprops")?.has("disambiguation") == true) continue
                val title = page.optString("title").trim()
                val extract = cleanDescription(page.optString("extract"))
                if (extract.length < MIN_DESCRIPTION_LENGTH) continue
                val score = wikipediaScore(album, title, extract)
                if (score > bestScore) {
                    bestScore = score
                    bestText = extract
                }
            }
            if (bestScore >= MIN_WIKIPEDIA_SCORE && !bestText.isNullOrBlank()) return bestText
        }
        return null
    }

    private fun wikidataDescription(album: AlbumHit, languageCode: String): String? {
        val url = "https://www.wikidata.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "wbsearchentities")
            .addQueryParameter("search", "${album.title} ${album.artist}")
            .addQueryParameter("language", languageCode)
            .addQueryParameter("uselang", languageCode)
            .addQueryParameter("type", "item")
            .addQueryParameter("limit", "8")
            .addQueryParameter("format", "json")
            .addQueryParameter("origin", "*")
            .build()
        val results = requestJson(url)?.optJSONArray("search") ?: return null
        val albumKey = descriptionKey(album.title)
        val artistKey = descriptionKey(album.artist)
        var best: String? = null
        var bestScore = Int.MIN_VALUE
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val label = item.optString("label").trim()
            val description = cleanDescription(item.optString("description"))
            if (description.length < 12) continue
            val labelKey = descriptionKey(label)
            val descriptionKey = descriptionKey(description)
            var score = 0
            if (labelKey == albumKey) score += 120
            else if (labelKey.contains(albumKey) || albumKey.contains(labelKey)) score += 70
            if (artistKey.isNotBlank() && descriptionKey.contains(artistKey)) score += 70
            if (descriptionKey.contains("album")) score += 30
            if (score > bestScore) {
                bestScore = score
                best = description
            }
        }
        return best?.takeIf { bestScore >= MIN_WIKIDATA_SCORE }
    }

    private fun spotifyDescription(canonicalUrl: String): String? {
        val url = canonicalUrl.trim().toHttpUrlOrNull() ?: return null
        if (!url.isHttps || !url.host.equals("open.spotify.com", ignoreCase = true)) return null
        if (url.pathSegments.firstOrNull() != "album") return null
        val html = requestText(url, "text/html") ?: return null
        val description = sequenceOf(
            SPOTIFY_OG_DESCRIPTION.find(html)?.groupValues?.getOrNull(1),
            SPOTIFY_OG_DESCRIPTION_REVERSED.find(html)?.groupValues?.getOrNull(1),
            SPOTIFY_TWITTER_DESCRIPTION.find(html)?.groupValues?.getOrNull(1)
        ).filterNotNull().map(::decodeHtml).map(::cleanDescription).firstOrNull { it.length >= MIN_DESCRIPTION_LENGTH }
            ?: return null
        val normalized = description.lowercase(Locale.ROOT)
        if (normalized.startsWith("listen to ") && normalized.contains(" on spotify")) return null
        return description
    }

    private fun requestJson(url: HttpUrl): JSONObject? {
        val body = requestText(url, "application/json") ?: return null
        return runCatching { JSONObject(body) }.getOrNull()
    }

    private fun requestText(url: HttpUrl, accept: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", accept)
            .header("Accept-Language", "en-US,en;q=0.8")
            .header("User-Agent", "Levyra/Android album-metadata")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()?.take(MAX_RESPONSE_CHARS)
            }
        }.getOrNull()
    }

    private fun wikipediaScore(album: AlbumHit, pageTitle: String, extract: String): Int {
        val albumKey = descriptionKey(album.title)
        val artistKey = descriptionKey(album.artist)
        val titleKey = descriptionKey(pageTitle.substringBeforeLast(" ("))
        val extractKey = descriptionKey(extract)
        var score = 0
        if (titleKey == albumKey) score += 130
        else if (titleKey.contains(albumKey) || albumKey.contains(titleKey)) score += 80
        if (artistKey.isNotBlank() && extractKey.contains(artistKey)) score += 75
        if (extractKey.contains("album")) score += 25
        if (pageTitle.contains("song", ignoreCase = true) || pageTitle.contains("singolo", ignoreCase = true)) score -= 90
        return score
    }

    private fun factualFallback(album: AlbumHit, languageCode: String, trackCount: Int): String {
        val year = album.year.ifBlank { album.releaseDate.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }.orEmpty() }
        return when (languageCode) {
            "it" -> buildString {
                append(album.title).append(" è un album di ").append(album.artist)
                if (year.isNotBlank()) append(", pubblicato nel ").append(year)
                if (trackCount > 0) append(". Contiene ").append(trackCount).append(if (trackCount == 1) " brano" else " brani")
                append('.')
            }
            "es" -> buildString {
                append(album.title).append(" es un álbum de ").append(album.artist)
                if (year.isNotBlank()) append(", publicado en ").append(year)
                if (trackCount > 0) append(". Incluye ").append(trackCount).append(if (trackCount == 1) " canción" else " canciones")
                append('.')
            }
            "fr" -> buildString {
                append(album.title).append(" est un album de ").append(album.artist)
                if (year.isNotBlank()) append(", publié en ").append(year)
                if (trackCount > 0) append(". Il contient ").append(trackCount).append(if (trackCount == 1) " titre" else " titres")
                append('.')
            }
            "de" -> buildString {
                append(album.title).append(" ist ein Album von ").append(album.artist)
                if (year.isNotBlank()) append(" aus dem Jahr ").append(year)
                if (trackCount > 0) append(". Es enthält ").append(trackCount).append(if (trackCount == 1) " Titel" else " Titel")
                append('.')
            }
            "pt" -> buildString {
                append(album.title).append(" é um álbum de ").append(album.artist)
                if (year.isNotBlank()) append(", lançado em ").append(year)
                if (trackCount > 0) append(". Contém ").append(trackCount).append(if (trackCount == 1) " faixa" else " faixas")
                append('.')
            }
            else -> buildString {
                append(album.title).append(" is an album by ").append(album.artist)
                if (year.isNotBlank()) append(", released in ").append(year)
                if (trackCount > 0) append(". It contains ").append(trackCount).append(if (trackCount == 1) " track" else " tracks")
                append('.')
            }
        }
    }

    private fun cleanDescription(value: String): String = decodeHtml(value)
        .replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(MAX_DESCRIPTION_CHARS)

    private fun descriptionKey(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun decodeHtml(value: String): String = value
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")

    private companion object {
        const val REMOTE_TIMEOUT_MS = 7_000L
        const val MIN_DESCRIPTION_LENGTH = 24
        const val MIN_WIKIPEDIA_SCORE = 130
        const val MIN_WIKIDATA_SCORE = 120
        const val MAX_RESPONSE_CHARS = 240_000
        const val MAX_DESCRIPTION_CHARS = 900
        val SPOTIFY_OG_DESCRIPTION = Regex(
            "<meta[^>]+(?:property|name)=[\\\"']og:description[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"']",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val SPOTIFY_OG_DESCRIPTION_REVERSED = Regex(
            "<meta[^>]+content=[\\\"']([^\\\"']+)[\\\"'][^>]+(?:property|name)=[\\\"']og:description[\\\"']",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val SPOTIFY_TWITTER_DESCRIPTION = Regex(
            "<meta[^>]+(?:property|name)=[\\\"']twitter:description[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"']",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
    }
}
'''

DESCRIPTION.parent.mkdir(parents=True, exist_ok=True)
DESCRIPTION.write_text(DESCRIPTION_CONTENT, encoding="utf-8")

replace_once(
    YOUTUBE,
    "    private val watchRepository = YoutubeMusicWatchRepository(context)\n    private val resilienceClient = YoutubeMusicResilienceClient(context, apiKey, clientVersion)\n",
    "    private val watchRepository = YoutubeMusicWatchRepository(context)\n    private val resilienceClient = YoutubeMusicResilienceClient(context, apiKey, clientVersion)\n    private val albumDescriptionRepository = AlbumDescriptionRepository(context)\n",
)

replace_regex_once(
    YOUTUBE,
    r"    suspend fun homeAlbums\(.*?\n    suspend fun moodCategories\(",
    r'''    suspend fun homeAlbums(
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 20,
        seeds: List<AlbumRecommendationSeed> = emptyList(),
        concurrency: Int = ALBUM_RECOMMENDATION_CONCURRENCY
    ): List<AlbumHit> = withContext(Dispatchers.IO) {
        val boundedLimit = limit.coerceIn(1, 40)
        val normalizedSeeds = seeds
            .asSequence()
            .map { seed -> seed.copy(query = seed.query.trim(), weight = seed.weight.coerceIn(0, 2_000)) }
            .filter { it.query.length >= 2 }
            .distinctBy { seed ->
                listOf(
                    albumRecommendationTextKey(seed.query),
                    albumRecommendationTextKey(seed.artist),
                    albumRecommendationTextKey(seed.album),
                    seed.moodTags.map(::albumRecommendationTextKey).sorted().joinToString("|")
                ).joinToString("|")
            }
            .take(MAX_ALBUM_RECOMMENDATION_SEEDS)
            .toList()
        val personalized = if (normalizedSeeds.isEmpty()) {
            emptyList()
        } else {
            val limiter = Semaphore(concurrency.coerceIn(1, ALBUM_RECOMMENDATION_CONCURRENCY))
            coroutineScope {
                normalizedSeeds.map { seed ->
                    async {
                        limiter.withPermit {
                            runCatching {
                                searchAlbumHits(seed.query, languageCode, ALBUM_RESULTS_PER_SEED)
                                    .mapIndexedNotNull { index, album ->
                                        val matchScore = levyraAlbumRecommendationMatchScore(album, seed)
                                        if (matchScore == LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE) null
                                        else ScoredAlbumRecommendation(
                                            album = album,
                                            score = seed.weight + matchScore - index * ALBUM_RESULT_RANK_PENALTY
                                        )
                                    }
                            }.getOrDefault(emptyList())
                        }
                    }
                }.awaitAll().flatten()
            }
                .groupBy { albumRecommendationDeduplicationKey(it.album) }
                .values
                .mapNotNull { group -> group.maxByOrNull { it.score } }
                .sortedWith(
                    compareByDescending<ScoredAlbumRecommendation> { it.score }
                        .thenBy { albumRecommendationTextKey(it.album.artist) }
                        .thenBy { albumRecommendationTextKey(it.album.title) }
                )
                .map { it.album }
        }
        val homeAlbums = runCatching { homeAlbumFeedInnerTube(languageCode) }.getOrDefault(emptyList())
        val baseAlbums = (personalized + homeAlbums)
            .asSequence()
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() }
            .filter { it.browseId.isNotBlank() || it.query.isNotBlank() }
            .distinctBy(::albumRecommendationDeduplicationKey)
            .toList()
        val fallbackAlbums = if (baseAlbums.size >= boundedLimit) {
            emptyList()
        } else {
            val limiter = Semaphore(concurrency.coerceIn(1, ALBUM_RECOMMENDATION_CONCURRENCY))
            coroutineScope {
                albumRecommendationQueries(languageCode).map { query ->
                    async {
                        limiter.withPermit {
                            runCatching {
                                searchAlbumHits(query, languageCode, ALBUM_RESULTS_PER_FALLBACK_QUERY)
                            }.getOrDefault(emptyList())
                        }
                    }
                }.awaitAll().flatten()
            }
        }
        (baseAlbums + fallbackAlbums)
            .asSequence()
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() }
            .filter { it.browseId.isNotBlank() || it.query.isNotBlank() }
            .distinctBy(::albumRecommendationDeduplicationKey)
            .take(boundedLimit)
            .toList()
    }

    suspend fun moodCategories(''',
)

replace_once(
    YOUTUBE,
    "                .map { track -> track.copy(album = headerAlbum.title, thumbnailUrl = track.thumbnailUrl.ifBlank { cover }, largeThumbnailUrl = track.largeThumbnailUrl.ifBlank { cover }) }",
    "                .map { track ->\n                    track.copy(\n                        album = headerAlbum.title,\n                        thumbnailUrl = cover.ifBlank { track.thumbnailUrl },\n                        largeThumbnailUrl = cover.ifBlank { track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } }\n                    )\n                }",
)

replace_once(
    YOUTUBE,
    "                thumbnailUrl = track.thumbnailUrl.ifBlank { finalAlbum.thumbnailUrl },\n                largeThumbnailUrl = track.largeThumbnailUrl.ifBlank { finalAlbum.thumbnailUrl }",
    "                thumbnailUrl = finalAlbum.thumbnailUrl.ifBlank { track.thumbnailUrl },\n                largeThumbnailUrl = finalAlbum.thumbnailUrl.ifBlank { track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } }",
)

replace_once(
    YOUTUBE,
    "        enrichedTracks.forEach { memory[it.id] = it }\n        AlbumDetail(\n            album = finalAlbum,\n            description = root?.let { parseAlbumDescription(it) }.orEmpty(),",
    "        enrichedTracks.forEach { memory[it.id] = it }\n        val description = albumDescriptionRepository.resolve(\n            album = finalAlbum,\n            languageCode = languageCode,\n            youtubeDescription = root?.let { parseAlbumDescription(it) }.orEmpty(),\n            trackCount = enrichedTracks.size\n        )\n        AlbumDetail(\n            album = finalAlbum,\n            description = description,",
)

replace_once(
    YOUTUBE,
    "        val thumbnail = findBestThumbnail(renderer).ifBlank { album.thumbnailUrl }",
    "        val thumbnail = album.thumbnailUrl.ifBlank { findBestThumbnail(renderer) }",
)

replace_once(YOUTUBE, "private const val MAX_ALBUM_RECOMMENDATION_SEEDS = 12", "private const val MAX_ALBUM_RECOMMENDATION_SEEDS = 16")
replace_once(YOUTUBE, "private const val ALBUM_RECOMMENDATION_CONCURRENCY = 3", "private const val ALBUM_RECOMMENDATION_CONCURRENCY = 4")
replace_once(YOUTUBE, "private const val ALBUM_RESULTS_PER_SEED = 5", "private const val ALBUM_RESULTS_PER_SEED = 8")
replace_once(YOUTUBE, "private const val ALBUM_RESULTS_PER_FALLBACK_QUERY = 4", "private const val ALBUM_RESULTS_PER_FALLBACK_QUERY = 8")

replace_once(
    VIEWMODEL,
    "            val localizedSeedNames = LevyraContentLocales.artistSuggestions(languageCode)",
    "            val localizedSeedNames = (\n                LevyraContentLocales.artistSuggestions(languageCode) + GLOBAL_HOME_ARTIST_FALLBACKS\n            ).distinctBy(::artistIdentityKey)",
)

replace_once(
    VIEWMODEL,
    "            val remoteLimit = if (deferUntilHomeIdle) {\n                startupPlan.albumCandidateCount\n            } else {\n                HOME_ALBUM_REMOTE_CANDIDATE_LIMIT\n            }",
    "            val remoteLimit = if (deferUntilHomeIdle) {\n                maxOf(HOME_ALBUM_RECOMMENDATION_LIMIT, startupPlan.albumCandidateCount)\n            } else {\n                HOME_ALBUM_REMOTE_CANDIDATE_LIMIT\n            }",
)

replace_once(VIEWMODEL, "        if (ranked.isEmpty()) return emptyList()", "        if (ranked.isEmpty()) return candidates.take(limit)")
replace_once(
    VIEWMODEL,
    "        if (selected.size < limit) {\n            ranked.asSequence()\n                .map { scored: Pair<AlbumHit, Int> -> scored.first }",
    "        if (selected.size < limit) {\n            candidates.asSequence()",
)

replace_once(VIEWMODEL, "        private const val HOME_ARTIST_SHELF_SIZE = 13", "        private const val HOME_ARTIST_SHELF_SIZE = 20")
replace_once(VIEWMODEL, "        private const val HOME_ARTIST_HISTORY_LIMIT = 48", "        private const val HOME_ARTIST_HISTORY_LIMIT = 72")
replace_once(VIEWMODEL, "        private const val HOME_ARTIST_CANDIDATE_LIMIT = 48", "        private const val HOME_ARTIST_CANDIDATE_LIMIT = 72")
replace_once(VIEWMODEL, "        private const val HOME_ARTIST_RESOLUTION_CONCURRENCY = 2", "        private const val HOME_ARTIST_RESOLUTION_CONCURRENCY = 4")
replace_once(VIEWMODEL, "        private const val HOME_ARTIST_TOTAL_TIMEOUT_MS = 9_000L", "        private const val HOME_ARTIST_TOTAL_TIMEOUT_MS = 18_000L")
replace_once(VIEWMODEL, "        private const val HOME_ALBUM_RECOMMENDATION_LIMIT = 10", "        private const val HOME_ALBUM_RECOMMENDATION_LIMIT = 20")
replace_once(VIEWMODEL, "        private const val HOME_ALBUM_REMOTE_CANDIDATE_LIMIT = 24", "        private const val HOME_ALBUM_REMOTE_CANDIDATE_LIMIT = 32")
replace_once(VIEWMODEL, "        private const val HOME_ALBUM_REMOTE_CONCURRENCY = 3", "        private const val HOME_ALBUM_REMOTE_CONCURRENCY = 4")
replace_once(VIEWMODEL, "        private const val HOME_ALBUM_SEED_LIMIT = 12", "        private const val HOME_ALBUM_SEED_LIMIT = 16")

replace_once(
    VIEWMODEL,
    "        private const val OFFICIAL_METADATA_MAX_BATCH_SIZE = 8",
    "        private val GLOBAL_HOME_ARTIST_FALLBACKS = listOf(\n            \"The Weeknd\", \"Drake\", \"Taylor Swift\", \"Billie Eilish\", \"SZA\",\n            \"Travis Scott\", \"Dua Lipa\", \"Post Malone\", \"Ariana Grande\", \"Kendrick Lamar\",\n            \"Bruno Mars\", \"Beyoncé\", \"Rihanna\", \"Ed Sheeran\", \"Lady Gaga\",\n            \"Bad Bunny\", \"Doja Cat\", \"Coldplay\", \"Imagine Dragons\", \"Lana Del Rey\",\n            \"Olivia Rodrigo\", \"Sabrina Carpenter\", \"Miley Cyrus\", \"Harry Styles\"\n        )\n        private const val OFFICIAL_METADATA_MAX_BATCH_SIZE = 8",
)

print("Album artwork, descriptions and 20-item home shelves patched successfully")
