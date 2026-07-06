package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Track
import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import kotlin.math.absoluteValue

object LevyraStartupCatalog {
    fun homeSections(): List<HomeSection> {
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
        val italian = listOf(
            track("LA FINE DEL MONDO", "Sfera Ebbasta", "YouTube Music", "", setOf("rap", "italia", "hit"), 86, 70, 90),
            track("Tuta Gold", "Mahmood", "YouTube Music", "", setOf("pop", "italia", "hit"), 82, 76, 88),
            track("Cenere", "Lazza", "YouTube Music", "", setOf("rap", "italia", "melodic"), 80, 75, 89),
            track("Bellissima", "Annalisa", "YouTube Music", "", setOf("pop", "italia", "dance"), 84, 77, 87),
            track("Pastello Bianco", "Pinguini Tattici Nucleari", "YouTube Music", "", setOf("pop", "italia", "chill"), 66, 74, 84),
            track("Superclassico", "Ernia", "YouTube Music", "", setOf("rap", "italia", "mood"), 72, 70, 82)
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
            HomeSection("Scelte rapide", quick),
            HomeSection("Italia nella tua orbita", italian),
            HomeSection("Energia immediata", focus)
        )
    }

    fun chartTracks(): List<Track> = homeSections().flatMap { it.tracks }.distinctBy { it.title.lowercase() to it.artist.lowercase() }.take(20)

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
            videoUrl = "",
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
