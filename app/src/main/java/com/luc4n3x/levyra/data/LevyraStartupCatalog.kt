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
            HomeSection(locale.localSectionTitle, localTracks(languageCode)),
            HomeSection(locale.energySectionTitle, focus)
        )
    }

    fun chartTracks(languageCode: String = LevyraLanguageCatalog.deviceDefault()): List<Track> = homeSections(languageCode).flatMap { it.tracks }.distinctBy { it.title.lowercase() to it.artist.lowercase() }.take(20)

    private fun localTracks(languageCode: String): List<Track> {
        return when (LevyraLanguageCatalog.normalize(languageCode)) {
            "it" -> listOf(
                track("LA FINE DEL MONDO", "Sfera Ebbasta", "YouTube Music", "", setOf("rap", "local", "hit"), 86, 70, 90),
                track("Tuta Gold", "Mahmood", "YouTube Music", "", setOf("pop", "local", "hit"), 82, 76, 88),
                track("Cenere", "Lazza", "YouTube Music", "", setOf("rap", "local", "melodic"), 80, 75, 89),
                track("Bellissima", "Annalisa", "YouTube Music", "", setOf("pop", "local", "dance"), 84, 77, 87),
                track("Pastello Bianco", "Pinguini Tattici Nucleari", "YouTube Music", "", setOf("pop", "local", "chill"), 66, 74, 84),
                track("Superclassico", "Ernia", "YouTube Music", "", setOf("rap", "local", "mood"), 72, 70, 82)
            )
            "es" -> listOf(
                track("Despacito", "Luis Fonsi", "YouTube Music", "", setOf("latin", "local", "hit"), 88, 72, 96),
                track("TQG", "KAROL G & Shakira", "YouTube Music", "", setOf("latin", "reggaeton", "hit"), 86, 74, 92),
                track("BZRP Music Sessions #53", "Shakira & Bizarrap", "YouTube Music", "", setOf("latin", "pop", "hit"), 84, 78, 93),
                track("LALA", "Myke Towers", "YouTube Music", "", setOf("reggaeton", "party", "hit"), 85, 70, 90),
                track("La Bachata", "Manuel Turizo", "YouTube Music", "", setOf("latin", "chill", "local"), 72, 76, 88),
                track("Mi Gente", "J Balvin & Willy William", "YouTube Music", "", setOf("party", "latin", "dance"), 90, 66, 92)
            )
            "fr" -> listOf(
                track("Dernière danse", "Indila", "YouTube Music", "", setOf("pop", "local", "mood"), 70, 82, 92),
                track("Alors on danse", "Stromae", "YouTube Music", "", setOf("dance", "local", "hit"), 86, 70, 94),
                track("Djadja", "Aya Nakamura", "YouTube Music", "", setOf("pop", "local", "hit"), 82, 74, 90),
                track("Formidable", "Stromae", "YouTube Music", "", setOf("pop", "mood", "local"), 62, 84, 88),
                track("Sapés comme jamais", "Maître Gims", "YouTube Music", "", setOf("rap", "party", "local"), 84, 76, 87),
                track("Jolie", "Ninho", "YouTube Music", "", setOf("rap", "local", "hit"), 78, 72, 86)
            )
            "de" -> listOf(
                track("Roller", "Apache 207", "YouTube Music", "", setOf("rap", "local", "hit"), 84, 72, 92),
                track("Komet", "Udo Lindenberg & Apache 207", "YouTube Music", "", setOf("pop", "local", "hit"), 76, 80, 91),
                track("Wildberry Lillet", "Nina Chuba", "YouTube Music", "", setOf("pop", "party", "local"), 82, 74, 88),
                track("Atemlos durch die Nacht", "Helene Fischer", "YouTube Music", "", setOf("pop", "party", "local"), 88, 78, 90),
                track("99 Luftballons", "Nena", "YouTube Music", "", setOf("classic", "pop", "local"), 80, 76, 92),
                track("Ohne mein Team", "Bonez MC & RAF Camora", "YouTube Music", "", setOf("rap", "local", "energy"), 86, 70, 87)
            )
            "pt" -> listOf(
                track("Envolver", "Anitta", "YouTube Music", "", setOf("pop", "local", "hit"), 84, 72, 92),
                track("Ai Se Eu Te Pego", "Michel Teló", "YouTube Music", "", setOf("party", "local", "hit"), 90, 70, 94),
                track("Acorda Pedrinho", "Jovem Dionisio", "YouTube Music", "", setOf("pop", "local", "chill"), 74, 76, 88),
                track("Malvadão 3", "Xamã", "YouTube Music", "", setOf("rap", "local", "hit"), 82, 72, 87),
                track("Cheguei", "Ludmilla", "YouTube Music", "", setOf("funk", "party", "local"), 88, 74, 86),
                track("Evidências", "Chitãozinho & Xororó", "YouTube Music", "", setOf("classic", "local", "vocal"), 62, 86, 91)
            )
            "nl" -> listOf(
                track("Europapa", "Joost", "YouTube Music", "", setOf("pop", "local", "hit"), 88, 70, 92),
                track("Friesenjung", "Ski Aggu, Joost & Otto Waalkes", "YouTube Music", "", setOf("party", "local", "hit"), 90, 68, 88),
                track("Leef", "André Hazes Jr.", "YouTube Music", "", setOf("pop", "local", "classic"), 78, 82, 86),
                track("Drank & Drugs", "Lil Kleine & Ronnie Flex", "YouTube Music", "", setOf("rap", "local", "party"), 86, 68, 87),
                track("Links Rechts", "Snollebollekes", "YouTube Music", "", setOf("party", "local", "dance"), 94, 66, 86),
                track("Stiekem", "Maan & Goldband", "YouTube Music", "", setOf("pop", "local", "hit"), 76, 78, 84)
            )
            "pl" -> listOf(
                track("Solo", "Blanka", "YouTube Music", "", setOf("pop", "local", "hit"), 82, 74, 88),
                track("Szampan", "sanah", "YouTube Music", "", setOf("pop", "local", "hit"), 74, 82, 90),
                track("Ale jazz!", "sanah & Vito Bambino", "YouTube Music", "", setOf("pop", "local", "chill"), 72, 78, 87),
                track("Supermoce", "Męskie Granie Orkiestra", "YouTube Music", "", setOf("pop", "local", "anthem"), 78, 82, 86),
                track("Za krokiem krok", "Cleo", "YouTube Music", "", setOf("pop", "local", "dance"), 84, 76, 84),
                track("Malomiasteczkowy", "Dawid Podsiadło", "YouTube Music", "", setOf("pop", "local", "mood"), 70, 80, 89)
            )
            "ro" -> listOf(
                track("Dragostea Din Tei", "O-Zone", "YouTube Music", "", setOf("pop", "local", "classic"), 88, 72, 96),
                track("Sub pielea mea", "Carla's Dreams", "YouTube Music", "", setOf("pop", "local", "hit"), 78, 76, 90),
                track("Made in Romania", "Ionut Cercel", "YouTube Music", "", setOf("party", "local", "hit"), 92, 70, 88),
                track("Copacul", "Aurelian Temișan", "YouTube Music", "", setOf("classic", "local", "vocal"), 62, 84, 84),
                track("Până vara viitoare", "Jo", "YouTube Music", "", setOf("pop", "local", "chill"), 70, 78, 84),
                track("Dale", "Andra", "YouTube Music", "", setOf("pop", "party", "local"), 84, 76, 84)
            )
            "el" -> listOf(
                track("Zari", "Marina Satti", "YouTube Music", "", setOf("pop", "local", "hit"), 86, 78, 90),
                track("My Number One", "Helena Paparizou", "YouTube Music", "", setOf("pop", "local", "classic"), 86, 78, 90),
                track("Dynata", "Antonis Remos", "YouTube Music", "", setOf("pop", "local", "classic"), 72, 84, 86),
                track("Gia", "Despina Vandi", "YouTube Music", "", setOf("party", "local", "pop"), 88, 74, 88),
                track("An Me Dis Na Kleo", "Sakis Rouvas", "YouTube Music", "", setOf("pop", "local", "mood"), 70, 82, 84),
                track("Opa", "Giorgos Alkaios", "YouTube Music", "", setOf("party", "local", "dance"), 92, 72, 86)
            )
            "sv" -> listOf(
                track("Tattoo", "Loreen", "YouTube Music", "", setOf("pop", "local", "hit"), 86, 82, 94),
                track("Euphoria", "Loreen", "YouTube Music", "", setOf("dance", "local", "hit"), 90, 82, 96),
                track("Wake Me Up", "Avicii", "YouTube Music", "", setOf("electronic", "local", "classic"), 88, 72, 96),
                track("Bara få va mig själv", "Laleh", "YouTube Music", "", setOf("pop", "local", "anthem"), 76, 82, 88),
                track("Främling", "Carola", "YouTube Music", "", setOf("classic", "local", "pop"), 74, 84, 86),
                track("Gimme! Gimme! Gimme!", "ABBA", "YouTube Music", "", setOf("classic", "party", "local"), 88, 78, 98)
            )
            "da" -> listOf(
                track("7 Years", "Lukas Graham", "YouTube Music", "", setOf("pop", "local", "hit"), 70, 82, 94),
                track("Druk igen", "Ude Af Kontrol", "YouTube Music", "", setOf("party", "local", "rap"), 90, 70, 86),
                track("Smuk som et stjerneskud", "Brødrene Olsen", "YouTube Music", "", setOf("classic", "local", "pop"), 72, 82, 86),
                track("Hvor Små Vi Er", "Rasmus Seebach", "YouTube Music", "", setOf("pop", "local", "mood"), 68, 84, 86),
                track("Only Teardrops", "Emmelie de Forest", "YouTube Music", "", setOf("pop", "local", "hit"), 78, 80, 88),
                track("Copenhagen", "Christopher", "YouTube Music", "", setOf("pop", "local", "chill"), 74, 78, 84)
            )
            "cs" -> listOf(
                track("Slzy tvoji mámy", "Olympic", "YouTube Music", "", setOf("rock", "local", "classic"), 76, 84, 88),
                track("Nonstop", "Michal David", "YouTube Music", "", setOf("party", "local", "classic"), 86, 74, 88),
                track("Boky jako skříň", "Ewa Farna", "YouTube Music", "", setOf("pop", "local", "hit"), 82, 78, 86),
                track("Malibu", "Viktor Sheen & Calin", "YouTube Music", "", setOf("rap", "local", "hit"), 80, 72, 86),
                track("Anděl", "Karel Kryl", "YouTube Music", "", setOf("classic", "local", "vocal"), 58, 86, 84),
                track("Holubí dům", "Jiří Schelinger", "YouTube Music", "", setOf("rock", "local", "classic"), 72, 84, 86)
            )
            "uk" -> listOf(
                track("Stefania", "Kalush Orchestra", "YouTube Music", "", setOf("pop", "local", "hit"), 86, 78, 94),
                track("Shum", "Go_A", "YouTube Music", "", setOf("electronic", "local", "energy"), 90, 76, 92),
                track("Teresa & Maria", "alyona alyona & Jerry Heil", "YouTube Music", "", setOf("pop", "local", "hit"), 76, 84, 90),
                track("Dumka", "Jerry Heil", "YouTube Music", "", setOf("pop", "local", "mood"), 72, 82, 86),
                track("Ой у лузі червона калина", "Andriy Khlyvnyuk", "YouTube Music", "", setOf("local", "classic", "vocal"), 64, 88, 86),
                track("Місто весни", "Океан Ельзи", "YouTube Music", "", setOf("rock", "local", "mood"), 74, 84, 88)
            )
            else -> listOf(
                track("As It Was", "Harry Styles", "Harry's House", "H5v3kku4y6Q", setOf("pop", "hit", "local"), 82, 78, 94),
                track("Flowers", "Miley Cyrus", "Endless Summer Vacation", "G7KNmW9a75Y", setOf("pop", "hit", "local"), 80, 80, 94),
                track("Anti-Hero", "Taylor Swift", "Midnights", "b1kbLwvqugk", setOf("pop", "hit", "local"), 76, 82, 92),
                track("Heat Waves", "Glass Animals", "Dreamland", "mRD0-GxqHVo", setOf("pop", "chill", "hit"), 78, 76, 92),
                track("Sunflower", "Post Malone & Swae Lee", "Spider-Man", "ApXoWvfEYVU", setOf("pop", "rap", "chill"), 74, 72, 94),
                track("Uptown Funk", "Mark Ronson ft. Bruno Mars", "Uptown Special", "OPf0YbXqDm0", setOf("party", "pop", "hit"), 92, 74, 96)
            )
        }
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
