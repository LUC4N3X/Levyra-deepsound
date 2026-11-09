package com.luc4n3x.levyra.ui.i18n

internal data class MonoPlayerCopy(
    val upNext: String,
    val lyricsTab: String,
    val similar: String,
    val lyricsPreview: String,
    val showLyrics: String,
    val aboutArtist: String,
    val credits: String,
    val mainArtist: String,
    val moveUp: String,
    val moveDown: String,
    val explicit: String,
    val syncedLyrics: String
)

internal object LevyraMonoPlayerStrings {
    private val copies = mapOf(
        "en" to MonoPlayerCopy(
            upNext = "Up next",
            lyricsTab = "Lyrics",
            similar = "Similar",
            lyricsPreview = "Lyrics preview",
            showLyrics = "Show lyrics",
            aboutArtist = "About the artist",
            credits = "Credits",
            mainArtist = "Main artist",
            moveUp = "Move up",
            moveDown = "Move down",
            explicit = "Explicit",
            syncedLyrics = "Synced"
        ),
        "it" to MonoPlayerCopy(
            upNext = "A seguire",
            lyricsTab = "Testo",
            similar = "Simili",
            lyricsPreview = "Anteprima testo",
            showLyrics = "Mostra testo",
            aboutArtist = "L'artista",
            credits = "Crediti",
            mainArtist = "Artista principale",
            moveUp = "Sposta su",
            moveDown = "Sposta giù",
            explicit = "Esplicito",
            syncedLyrics = "Sincronizzato"
        )
    )

    fun forCode(code: String): MonoPlayerCopy = localizedValueOrEnglish(copies, code)
}
