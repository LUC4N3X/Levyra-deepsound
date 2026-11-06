package com.luc4n3x.levyra.ui.i18n

internal data class MonoSearchCopy(
    val voiceSearch: String,
    val lyricsSearch: String,
    val lastPlayed: String,
    val lastPlayedOverline: String,
    val recentOverline: String,
    val similarToTaste: String,
    val similarOverline: String,
    val suggestionsOverline: String,
    val resultsOverline: String,
    val topResultOverline: String,
    val artistOverline: String,
    val popular: String,
    val popularOverline: String,
    val discographyOverline: String,
    val videosOverline: String,
    val fansAlsoLike: String,
    val fansOverline: String,
    val about: String,
    val aboutOverline: String,
    val explicit: String
)

internal object LevyraMonoSearchStrings {
    private val copies = mapOf(
        "en" to MonoSearchCopy(
            voiceSearch = "Voice search",
            lyricsSearch = "Search by lyrics",
            lastPlayed = "Last played",
            lastPlayedOverline = "Pick up again",
            recentOverline = "History",
            similarToTaste = "Similar to your taste",
            similarOverline = "For you",
            suggestionsOverline = "Suggestions",
            resultsOverline = "Results",
            topResultOverline = "Top result",
            artistOverline = "Artist",
            popular = "Popular",
            popularOverline = "Most played",
            discographyOverline = "Discography",
            videosOverline = "On screen",
            fansAlsoLike = "Fans also like",
            fansOverline = "Nearby sounds",
            about = "About",
            aboutOverline = "Profile",
            explicit = "Explicit"
        ),
        "it" to MonoSearchCopy(
            voiceSearch = "Ricerca vocale",
            lyricsSearch = "Cerca per testo",
            lastPlayed = "Ultimi ascolti",
            lastPlayedOverline = "Riprendi da qui",
            recentOverline = "Cronologia",
            similarToTaste = "Simili ai tuoi gusti",
            similarOverline = "Per te",
            suggestionsOverline = "Suggerimenti",
            resultsOverline = "Risultati",
            topResultOverline = "Risultato migliore",
            artistOverline = "Artista",
            popular = "Popolari",
            popularOverline = "I più ascoltati",
            discographyOverline = "Discografia",
            videosOverline = "Sullo schermo",
            fansAlsoLike = "Ai fan piace anche",
            fansOverline = "Suoni vicini",
            about = "Informazioni",
            aboutOverline = "Profilo",
            explicit = "Esplicito"
        )
    )

    fun forCode(code: String): MonoSearchCopy = localizedValueOrEnglish(copies, code)
}
