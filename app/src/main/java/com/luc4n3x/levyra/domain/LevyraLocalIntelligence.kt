package com.luc4n3x.levyra.domain

import java.text.Normalizer
import java.util.Locale
import kotlin.math.roundToInt

class LevyraLocalIntelligence {
    fun analyze(track: Track, lines: List<LyricLine>): LevyraIntelligenceSummary {
        val sourceLines = lines.map { it.text.trim() }.filter { it.isNotBlank() }
        if (sourceLines.isEmpty()) {
            return LevyraIntelligenceSummary(
                overview = metadataOverview(track),
                mood = moodFromTrack(track),
                themes = track.moodTags.take(4).map(::displayToken),
                lineCount = 0,
                wordCount = 0
            )
        }
        val words = sourceLines.flatMap(::tokens)
        val meaningful = words.filterNot { it in stopWords || it.length < 3 }
        val frequencies = meaningful.groupingBy { it }.eachCount()
        val themes = frequencies.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key.length })
            .map { displayToken(it.key) }
            .distinct()
            .take(5)
        val repeatedPhrases = repeatedPhrases(sourceLines)
        val unique = meaningful.distinct().size
        val density = if (meaningful.isEmpty()) 0 else ((unique.toDouble() / meaningful.size.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        val mood = detectMood(words, track)
        val overview = buildString {
            append(sourceLines.size)
            append(if (sourceLines.size == 1) " verso" else " versi")
            append(", ")
            append(words.size)
            append(if (words.size == 1) " parola" else " parole")
            if (themes.isNotEmpty()) {
                append(". Temi dominanti: ")
                append(themes.joinToString(", "))
                append('.')
            }
            if (repeatedPhrases.isNotEmpty()) {
                append(" Ritornello o frase ricorrente: “")
                append(repeatedPhrases.first())
                append("”.")
            }
        }
        return LevyraIntelligenceSummary(
            overview = overview,
            mood = mood,
            themes = themes,
            repeatedPhrases = repeatedPhrases,
            lexicalDensity = density,
            lineCount = sourceLines.size,
            wordCount = words.size
        )
    }

    private fun metadataOverview(track: Track): String {
        val parts = buildList {
            if (track.title.isNotBlank()) add(track.title)
            if (track.artist.isNotBlank()) add("di ${track.artist}")
            if (track.album.isNotBlank()) add("dall'album ${track.album}")
        }
        return parts.joinToString(" ").ifBlank { "Analisi locale disponibile quando viene caricato il testo." }
    }

    private fun moodFromTrack(track: Track): String {
        return when {
            track.energy >= 75 && track.vocal >= 55 -> "Energetico e diretto"
            track.energy >= 75 -> "Intenso e ritmico"
            track.energy <= 35 && track.vocal >= 55 -> "Intimo e riflessivo"
            track.energy <= 35 -> "Calmo e atmosferico"
            else -> "Equilibrato"
        }
    }

    private fun detectMood(words: List<String>, track: Track): String {
        val positive = words.count { it in positiveWords }
        val negative = words.count { it in negativeWords }
        val intense = words.count { it in intenseWords }
        return when {
            intense >= 3 && intense > positive && intense > negative -> "Intenso e combattivo"
            positive >= negative + 3 -> "Luminoso e positivo"
            negative >= positive + 3 -> "Malinconico e introspettivo"
            track.energy >= 75 -> "Energico"
            track.energy <= 35 -> "Riflessivo"
            else -> "Emotivamente bilanciato"
        }
    }

    private fun repeatedPhrases(lines: List<String>): List<String> {
        val normalized = lines.map { normalizeLine(it) }.filter { it.length >= 8 }
        val counts = normalized.groupingBy { it }.eachCount()
        return counts.entries
            .filter { it.value >= 2 }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key.length })
            .map { entry -> lines.firstOrNull { normalizeLine(it) == entry.key }.orEmpty().trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)
    }

    private fun tokens(value: String): List<String> {
        return normalize(value)
            .split(' ')
            .map(String::trim)
            .filter(String::isNotBlank)
    }

    private fun normalizeLine(value: String): String = tokens(value).joinToString(" ")

    private fun normalize(value: String): String {
        return Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9àèéìòóùçñäöüß' ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun displayToken(value: String): String {
        return value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    private companion object {
        val stopWords = setOf(
            "che", "non", "con", "per", "una", "uno", "del", "della", "delle", "dei", "gli", "alla", "alle", "nel", "nella", "sono", "siamo", "sei", "siete", "era", "erano", "come", "piu", "mai", "poi", "qui", "quando", "questa", "questo", "quella", "quello", "anche", "ancora", "solo", "tutto", "tutti", "you", "the", "and", "that", "this", "with", "for", "your", "but", "not", "are", "was", "were", "all", "just", "from", "have", "has", "its", "can", "will", "dont", "im", "ive", "me", "my", "we", "our", "to", "of", "in", "on", "a", "i", "it", "is"
        )
        val positiveWords = setOf("amore", "love", "felice", "happy", "sorriso", "smile", "luce", "light", "libero", "free", "insieme", "together", "sogno", "dream", "vivo", "alive")
        val negativeWords = setOf("dolore", "pain", "solo", "alone", "pianto", "cry", "lacrime", "tears", "paura", "fear", "perso", "lost", "morte", "death", "vuoto", "empty", "addio", "goodbye")
        val intenseWords = setOf("fuoco", "fire", "lotta", "fight", "guerra", "war", "forte", "strong", "urlo", "scream", "corsa", "run", "sangue", "blood", "potere", "power")
    }
}
