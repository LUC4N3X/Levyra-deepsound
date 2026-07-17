package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.Track
import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import kotlin.math.absoluteValue

object LevyraStartupCatalog {
    fun homeSections(languageCode: String = LevyraLanguageCatalog.deviceDefault()): List<HomeSection> {
        val locale = LevyraContentLocales.forLanguage(languageCode)
        val quick = listOf(
            track("Bohemian Rhapsody", "Queen", "Greatest Hits", "fJ9rUzIMcZQ", setOf("rock", "classic", "vocal"), 78, 82, 98),
            track("Blinding Lights", "The Weeknd", "After Hours", "4NRXx6U8ABQ", setOf("pop", "night", "hit"), 84, 64, 96),
            track("Levitating", "Dua Lipa", "Future Nostalgia", "TUVcZfQe-Kw", setOf("pop", "dance", "hit"), 86, 72, 93),
            track("Smells Like Teen Spirit", "Nirvana", "Nevermind", "hTWKbfoikeg", setOf("rock", "alt", "energy"), 91, 68, 94),
            track("Lose Yourself", "Eminem", "8 Mile", "xFYQQPAOz7Y", setOf("rap", "focus", "gym"), 92, 78, 95),
            track("Billie Jean", "Michael Jackson", "Thriller", "Zi_XLOBDo_Y", setOf("pop", "classic", "groove"), 82, 68, 96),
            track("Numb", "Linkin Park", "Meteora", "kXYiU_JCYtU", setOf("rock", "alt", "energy"), 88, 76, 93),
            track("Viva La Vida", "Coldplay", "Viva La Vida", "dvgZkm1xWPE", setOf("pop", "anthem", "mood"), 74, 70, 90)
        )
        val focus = listOf(
            track("Midnight City", "M83", "Hurry Up, We're Dreaming", "dX3k_QDnzHE", setOf("electronic", "night", "drive"), 78, 45, 88),
            track("Starboy", "The Weeknd", "Starboy", "34Na4j8AVgA", setOf("pop", "night", "drive"), 84, 68, 91),
            track("Believer", "Imagine Dragons", "Evolve", "7wtfhZwyrcc", setOf("rock", "gym", "energy"), 90, 72, 89),
            track("One More Time", "Daft Punk", "Discovery", "FGBhQbmPwH8", setOf("electronic", "dance", "classic"), 88, 48, 92),
            track("Take On Me", "a-ha", "Hunting High and Low", "djV11Xbc914", setOf("pop", "classic", "drive"), 80, 72, 90),
            track("In The End", "Linkin Park", "Hybrid Theory", "eVTXPUF4Oz4", setOf("rock", "alt", "energy"), 86, 76, 93)
        )
        return listOf(
            HomeSection(locale.quickSectionTitle, quick),
            HomeSection(locale.energySectionTitle, focus)
        )
    }

    fun chartTracks(languageCode: String = LevyraLanguageCatalog.deviceDefault()): List<Track> = homeSections(languageCode).flatMap { it.tracks }.distinctBy { it.title.lowercase() to it.artist.lowercase() }.take(20)

    fun repairHomeSections(sections: List<HomeSection>, languageCode: String): List<HomeSection> {
        if (sections.isEmpty()) return sections
        return sections.map { section ->
            section.copy(tracks = repairTracks(section.tracks, languageCode))
        }
    }

    fun repairTracks(tracks: List<Track>, languageCode: String): List<Track> {
        if (tracks.isEmpty()) return tracks
        val canonical = homeSections(LevyraLanguageCatalog.normalize(languageCode)).flatMap { it.tracks }
        val exact = canonical.associateBy { seedTrackKey(it.title, it.artist) }
        return tracks.map { current ->
            if (!isStartupSeed(current)) return@map current
            val replacement = exact[seedTrackKey(current.title, current.artist)] ?: return@map current
            replacement.copy(
                durationMs = current.durationMs.takeIf { it > 0L } ?: replacement.durationMs,
                streamUrl = current.streamUrl,
                videoStreamUrl = current.videoStreamUrl,
                thumbnailUrl = current.thumbnailUrl.ifBlank { replacement.thumbnailUrl },
                largeThumbnailUrl = current.largeThumbnailUrl.ifBlank { replacement.largeThumbnailUrl },
                source = current.source.ifBlank { replacement.source },
                cacheScore = maxOf(current.cacheScore, replacement.cacheScore),
                sponsorSegments = current.sponsorSegments
            )
        }
    }

    private fun isStartupSeed(track: Track): Boolean {
        return track.source.equals("Levyra Start", ignoreCase = true) || track.id.startsWith("chart-seed-")
    }

    private fun seedTrackKey(title: String, artist: String): String = "${seedTitleKey(title)}|${seedTitleKey(artist)}"

    private fun seedTitleKey(value: String): String {
        return value.lowercase()
            .replace(Regex("""[^a-z0-9àèéìòóùçñäöüß\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun track(
        title: String,
        artist: String,
        album: String,
        videoId: String,
        tags: Set<String>,
        energy: Int,
        vocal: Int,
        replay: Int
    ): Track {
        val key = "$title|$artist"
        val seed = stableSeed(key)
        val palette = palette(seed)
        val art = if (videoId.isBlank()) "" else "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        return Track(
            id = "chart-seed-${stableId(key)}",
            title = title,
            artist = artist,
            album = album,
            durationMs = 0L,
            streamUrl = "",
            videoUrl = if (videoId.isBlank()) "" else "https://www.youtube.com/watch?v=$videoId",
            thumbnailUrl = art,
            largeThumbnailUrl = art,
            source = "Levyra Start",
            moodTags = tags,
            energy = energy.coerceIn(0, 100),
            vocal = vocal.coerceIn(0, 100),
            replayScore = replay.coerceIn(0, 100),
            cacheScore = 78,
            accentStart = palette.first,
            accentEnd = palette.second
        )
    }

    private fun stableSeed(value: String): Int {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .take(4)
            .fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            .absoluteValue
    }

    private fun stableId(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }

    private fun palette(seed: Int): Pair<Int, Int> {
        val palettes = listOf(
            0xFF00E5FF.toInt() to 0xFF7B42FF.toInt(),
            0xFF1B5CFF.toInt() to 0xFFFF4FD8.toInt(),
            0xFFFF7A18.toInt() to 0xFF8E57FF.toInt(),
            0xFF00D4A6.toInt() to 0xFFFF3B5C.toInt(),
            0xFFFFB000.toInt() to 0xFF00E5FF.toInt(),
            0xFF64FFDA.toInt() to 0xFF2979FF.toInt()
        )
        return palettes[((seed % palettes.size) + palettes.size) % palettes.size]
    }
}
