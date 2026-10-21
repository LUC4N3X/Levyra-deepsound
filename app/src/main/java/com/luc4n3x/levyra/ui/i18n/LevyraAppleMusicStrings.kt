package com.luc4n3x.levyra.ui.i18n

internal data class AppleMusicSkinCopy(
    val editorialUnavailable: String,
    val editorialRetry: String,
    val editorialOffline: String,
    val updated: String,
    val more: String,
    val explicit: String,
    val songs: (Int) -> String,
    val collectionUnavailable: String
)

internal object LevyraAppleMusicStrings {
    private val english = AppleMusicSkinCopy(
        editorialUnavailable = "Editorial shelves are not available right now.",
        editorialRetry = "Try again",
        editorialOffline = "Showing the last saved editorial shelves.",
        updated = "Updated",
        more = "More",
        explicit = "Explicit",
        songs = { count -> if (count == 1) "1 song" else "$count songs" },
        collectionUnavailable = "This list could not be loaded."
    )
    private val italian = AppleMusicSkinCopy(
        editorialUnavailable = "Le sezioni editoriali non sono disponibili in questo momento.",
        editorialRetry = "Riprova",
        editorialOffline = "Mostro le ultime sezioni editoriali salvate.",
        updated = "Aggiornato",
        more = "Altro",
        explicit = "Esplicito",
        songs = { count -> if (count == 1) "1 brano" else "$count brani" },
        collectionUnavailable = "Impossibile caricare questo elenco."
    )

    fun forCode(code: String): AppleMusicSkinCopy = if (code == "it") italian else english
}
