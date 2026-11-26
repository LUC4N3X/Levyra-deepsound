package com.luc4n3x.levyra.desktop.core.charts

import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.model.SearchFilter
import com.luc4n3x.levyra.desktop.core.model.Track
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class ChartVideo(val title: String, val artist: String)

class MusicVideoChart(
    private val catalog: CatalogRepository,
    private val client: OkHttpClient = ExtractorHttp.client
) {
    suspend fun trending(country: String, limit: Int = CHART_SIZE): List<Track> {
        val body = withContext(Dispatchers.IO) { fetch(feedUrl(country, limit)) } ?: return emptyList()
        val videos = parse(body, limit)
        val gate = Semaphore(SEARCH_PARALLELISM)
        return coroutineScope {
            videos.map { video ->
                async {
                    gate.withPermit {
                        searchQueries(video).firstNotNullOfOrNull { query ->
                            val candidates = try {
                                catalog.search(query, SearchFilter.VIDEOS).tracks
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                emptyList()
                            }
                            match(video, candidates)
                        }
                    }
                }
            }.awaitAll().filterNotNull().distinctBy { it.id }
        }
    }

    private fun fetch(url: String): String? = runCatching {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val body = response.body ?: return@use null
            if (body.contentLength() > MAX_BODY_BYTES) return@use null
            body.string().takeIf { it.length <= MAX_BODY_BYTES }
        }
    }.getOrNull()

    companion object {
        private const val CHART_SIZE = 20
        private const val SEARCH_PARALLELISM = 4
        private const val MAX_BODY_BYTES = 2L * 1024L * 1024L
        private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
        private val MARKS = Regex("\\p{M}+")
        private val ARTIST_SEPARATORS = Regex("\\s*(,|&|\\bx\\b|\\bfeat\\.?|\\bft\\.?|\\bwith\\b)\\s*", RegexOption.IGNORE_CASE)
        private val TITLE_DECORATION = Regex("\\s*[(\\[][^)\\]]*[)\\]]")
        private val VARIANT_WORDS = setOf("remix", "live", "cover", "karaoke", "lyric", "lyrics", "sped", "slowed", "instrumental", "acoustic", "reaction")

        fun feedUrl(country: String, limit: Int): String =
            "https://rss.marketingtools.apple.com/api/v2/${country.lowercase(Locale.ROOT)}/music/most-played/$limit/music-videos.json"

        fun parse(body: String, limit: Int): List<ChartVideo> {
            val results = runCatching { JSONObject(body).optJSONObject("feed")?.optJSONArray("results") }.getOrNull() ?: return emptyList()
            val videos = ArrayList<ChartVideo>()
            for (index in 0 until results.length()) {
                if (videos.size >= limit) break
                val entry = results.optJSONObject(index) ?: continue
                val title = entry.optString("name").trim()
                val artist = entry.optString("artistName").trim()
                if (title.isNotBlank() && artist.isNotBlank()) videos += ChartVideo(title, artist)
            }
            return videos.distinctBy { normalize(it.title) + "|" + normalize(it.artist) }
        }

        fun searchQueries(video: ChartVideo): List<String> {
            val cleanTitle = video.title.replace(TITLE_DECORATION, "").trim()
            val leadArtist = video.artist.split(ARTIST_SEPARATORS).firstOrNull { it.isNotBlank() }?.trim() ?: video.artist
            return listOf("${video.title} ${video.artist}", "$cleanTitle $leadArtist").distinct()
        }

        fun match(video: ChartVideo, candidates: List<Track>): Track? {
            val title = normalize(video.title.replace(TITLE_DECORATION, ""))
            val fullTitle = normalize(video.title)
            val artists = video.artist.split(ARTIST_SEPARATORS).map(::normalize).filter(String::isNotEmpty)
            if (title.isEmpty() || artists.isEmpty()) return null
            return candidates
                .mapIndexedNotNull { index, candidate -> score(title, fullTitle, artists, candidate)?.let { Triple(it, -index, candidate) } }
                .maxWithOrNull(compareBy<Triple<Int, Int, Track>> { it.first }.thenBy { it.second })
                ?.third
        }

        private fun score(title: String, fullTitle: String, artists: List<String>, candidate: Track): Int? {
            val candidateArtist = normalize(candidate.artist)
            val candidateArtists = candidate.artist.split(ARTIST_SEPARATORS).map(::normalize).filter(String::isNotEmpty)
            val artistScore = when {
                candidateArtist.isEmpty() -> 0
                candidateArtists.any { it in artists } -> 3
                artists.any { it in candidateArtist || candidateArtist in it } -> 1
                else -> 0
            }
            if (artistScore == 0) return null
            val cleanCandidate = normalize(candidate.title.replace(TITLE_DECORATION, ""))
            val candidateTitle = normalize(candidate.title)
            val titleScore = when {
                cleanCandidate == title -> 4
                candidateTitle.isNotEmpty() && (title in candidateTitle || cleanCandidate in title && cleanCandidate.isNotEmpty()) -> 2
                else -> return null
            }
            val variantPenalty = VARIANT_WORDS.count { word -> word in candidateTitle.split(" ") && word !in fullTitle.split(" ") } * 3
            return artistScore + titleScore - variantPenalty
        }

        fun normalize(value: String): String =
            Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFKD)
                .replace(MARKS, "")
                .replace(NON_WORD, " ")
                .trim()
    }
}
