package com.luc4n3x.levyra.desktop.core.charts

import com.luc4n3x.levyra.desktop.core.model.Track
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ChartFeedParser {
    private const val CHART_ALBUM = "Apple Music Charts"
    private val artworkSize = Regex("""\d+x\d+bb""")
    private val parentheticals = Regex("""\((?:[^)]*)\)|\[(?:[^\]]*)]""")
    private val featuredMarkers = Regex("""\b(feat|ft|featuring|con|with)\b\.?""", RegexOption.IGNORE_CASE)
    private val videoMarkers = Regex(
        """\b(official|video|audio|lyric|lyrics|visualizer|remaster(?:ed)?|hd|4k)\b""",
        RegexOption.IGNORE_CASE
    )
    private val nonMusicCharacters = Regex("""[^\p{L}\p{Nd} ]""")
    private val repeatedWhitespace = Regex("""\s+""")

    private val json = Json { ignoreUnknownKeys = true }

    fun modern(body: String, limit: Int): List<Track> {
        val results = runCatching {
            json.parseToJsonElement(body).jsonObject["feed"]?.jsonObject?.get("results")?.jsonArray
        }.getOrNull() ?: return emptyList()
        return results.asSequence()
            .mapNotNull { element -> runCatching { element.jsonObject }.getOrNull() }
            .mapNotNull { entry ->
                val title = entry.text("name")
                if (title.isBlank()) {
                    null
                } else {
                    chartTrack(
                        title = title,
                        artist = entry.text("artistName"),
                        artwork = upgradeArtwork(entry.text("artworkUrl100"))
                    )
                }
            }
            .take(limit)
            .toList()
    }

    fun classic(body: String, limit: Int): List<Track> {
        val entries = runCatching {
            json.parseToJsonElement(body).jsonObject["feed"]?.jsonObject?.get("entry")?.jsonArray
        }.getOrNull() ?: return emptyList()
        return entries.asSequence()
            .mapNotNull { element -> runCatching { element.jsonObject }.getOrNull() }
            .mapNotNull { entry ->
                val title = entry.label("im:name")
                if (title.isBlank()) {
                    null
                } else {
                    val images = runCatching { entry["im:image"]?.jsonArray }.getOrNull()
                    val largest = images?.lastOrNull()?.let { image ->
                        runCatching { image.jsonObject["label"]?.jsonPrimitive?.content }.getOrNull()
                    }
                    chartTrack(
                        title = title,
                        artist = entry.label("im:artist"),
                        artwork = upgradeArtwork(largest.orEmpty())
                    )
                }
            }
            .take(limit)
            .toList()
    }

    fun upgradeArtwork(url: String): String {
        if (url.isBlank()) return url
        return url.replace(artworkSize, "600x600bb")
    }

    fun normalize(value: String): String = value.lowercase()
        .replace(parentheticals, " ")
        .replace(featuredMarkers, " ")
        .replace(videoMarkers, " ")
        .replace(nonMusicCharacters, " ")
        .replace(repeatedWhitespace, " ")
        .trim()

    fun chartId(title: String, artist: String): String {
        val identity = normalize("$title $artist").ifBlank { "$title$artist".lowercase() }
        return "chart-${identity.hashCode().toUInt().toString(16)}"
    }

    private fun chartTrack(title: String, artist: String, artwork: String): Track = Track(
        id = chartId(title, artist),
        title = title,
        artist = artist.ifBlank { "Vari artisti" },
        album = CHART_ALBUM,
        videoUrl = "",
        artworkUrl = artwork
    )

    private fun JsonObject.text(key: String): String =
        runCatching { this[key]?.jsonPrimitive?.content }.getOrNull().orEmpty().trim()

    private fun JsonObject.label(key: String): String =
        runCatching { this[key]?.jsonObject?.get("label")?.jsonPrimitive?.content }.getOrNull().orEmpty().trim()
}
