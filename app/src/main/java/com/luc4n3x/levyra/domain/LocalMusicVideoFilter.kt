package com.luc4n3x.levyra.domain

import java.lang.Character.UnicodeScript
import java.util.Locale

object LocalMusicVideoFilter {
    private val nonLatinScripts = mapOf(
        UnicodeScript.HAN to setOf("zh", "ja"),
        UnicodeScript.HIRAGANA to setOf("ja"),
        UnicodeScript.KATAKANA to setOf("ja"),
        UnicodeScript.HANGUL to setOf("ko"),
        UnicodeScript.CYRILLIC to setOf("ru", "uk", "bg", "sr", "kk"),
        UnicodeScript.ARABIC to setOf("ar", "fa"),
        UnicodeScript.HEBREW to setOf("he"),
        UnicodeScript.THAI to setOf("th"),
        UnicodeScript.DEVANAGARI to setOf("hi"),
        UnicodeScript.BENGALI to setOf("bn"),
        UnicodeScript.GREEK to setOf("el")
    )

    private val languageWords = mapOf(
        "es" to listOf("los dos", "de los", "de las", "corazón", "corridos", "cumbia", "bachata", "mariachi", "norteño", "banda sinaloense", "señor", "niña", "tamaulipas"),
        "pt" to listOf("não", "você", "coração", "kizomba", "sertanejo", "pagode", "forró", "piseiro", "saudade", "conselhos", "dos cotas"),
        "fr" to listOf("je", "les", "moi", "pas", "toujours", "chanson"),
        "de" to listOf("und", "ich", "nicht", "deutschrap", "schlager"),
        "tr" to listOf("aşk", "sevgilim", "türkçe"),
        "id" to listOf("dangdut"),
        "fil" to listOf("pinoy", "tagalog", "opm")
    )

    private val nonLetters = Regex("""[^\p{L}]+""")

    fun localFirst(tracks: List<Track>, languageCode: String): List<Track> {
        val language = LevyraContentLocales.contentLanguage(LevyraLanguageCatalog.normalize(languageCode)).substringBefore('-')
        val kept = tracks.filterNot { isForeign(it, language) }
        val (preferred, neutral) = kept.partition { LevyraPersonalOrbit.isLanguagePreferred(it, language) }
        return preferred + neutral
    }

    internal fun isForeign(track: Track, language: String): Boolean {
        val text = "${track.title} ${track.artist}"
        val foreignScript = text.codePoints().toArray().any { codePoint ->
            val script = runCatching { UnicodeScript.of(codePoint) }.getOrNull() ?: return@any false
            val owners = nonLatinScripts[script] ?: return@any false
            language !in owners
        }
        if (foreignScript) return true
        val joined = text.lowercase(Locale.ROOT)
        val words = joined.split(nonLetters).filter(String::isNotBlank).toSet()
        return languageWords.any { (owner, markers) ->
            owner != language && markers.any { marker -> if (' ' in marker) joined.contains(marker) else marker in words }
        }
    }
}
