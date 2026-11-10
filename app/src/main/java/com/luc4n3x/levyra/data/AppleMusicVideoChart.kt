package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import java.text.Normalizer
import org.json.JSONObject

data class AppleChartVideo(val title: String, val artist: String, val artworkUrl: String)

internal object AppleMusicVideoChart {
    private val ARTWORK_SIZE = Regex("\\d+x\\d+(bb)?")
    private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
    private val MARKS = Regex("\\p{M}+")
    private val ARTIST_SEPARATORS = Regex("\\s*(,|&|\\bx\\b|\\bfeat\\.?|\\bft\\.?|\\bwith\\b)\\s*", RegexOption.IGNORE_CASE)
    private val TITLE_DECORATION = Regex("\\s*[(\\[][^)\\]]*[)\\]]")
    private val VARIANT_WORDS = setOf("remix", "live", "cover", "karaoke", "lyric", "lyrics", "sped", "slowed", "instrumental", "acoustic", "reaction")

    fun feedUrl(country: String, limit: Int): String =
        "https://rss.marketingtools.apple.com/api/v2/${country.lowercase()}/music/most-played/$limit/music-videos.json"

    fun parse(body: String, limit: Int): List<AppleChartVideo> {
        val results = runCatching { JSONObject(body).optJSONObject("feed")?.optJSONArray("results") }.getOrNull()
            ?: return emptyList()
        val videos = ArrayList<AppleChartVideo>()
        for (index in 0 until results.length()) {
            if (videos.size >= limit) break
            val entry = results.optJSONObject(index) ?: continue
            val title = entry.optString("name").trim()
            val artist = entry.optString("artistName").trim()
            if (title.isBlank() || artist.isBlank()) continue
            val artwork = entry.optString("artworkUrl100").trim().replace(ARTWORK_SIZE, "1920x1080bb")
            videos += AppleChartVideo(title, artist, artwork)
        }
        return videos.distinctBy { normalize(it.title) + "|" + normalize(it.artist) }
    }

    fun match(video: AppleChartVideo, candidates: List<Track>): Track? {
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

    fun searchQueries(video: AppleChartVideo): List<String> {
        val cleanTitle = video.title.replace(TITLE_DECORATION, "").trim()
        val leadArtist = video.artist.split(ARTIST_SEPARATORS).firstOrNull { it.isNotBlank() }?.trim() ?: video.artist
        return listOf("${video.title} ${video.artist}", "$cleanTitle $leadArtist", "\"$cleanTitle\"").distinct()
    }

    fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFKD)
            .replace(MARKS, "")
            .replace(NON_WORD, " ")
            .trim()
}
