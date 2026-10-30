package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog

internal data class AtelierCopy(
    val skinBody: String,
    val shelf: String,
    val shelfEmptyTitle: String,
    val shelfEmptyBody: String,
    val playCollection: String,
    val openDetails: String,
    val previousSleeve: String,
    val nextSleeve: String,
    val shelfPosition: String,
    val tracksCount: String,
    val sideA: String,
    val goToShelf: String,
    val artistLabel: String = "Artist",
    val explicit: String = "Explicit",
    val queueUpNext: String = "Up next",
    val queueMoveUp: String = "Move up",
    val queueMoveDown: String = "Move down",
    val featureTitle: String = "Selections",
    val recentAlbums: String = "Recent albums",
    val crateTitle: String = "From the crate",
    val crateReroll: String = "Dig again",
    val openCollection: String = "Collection",
    val allTools: String = "All tools",
    val queueHistoryEmpty: String = "Nothing played yet",
    val retry: String = "Try again",
    val homeEmptyBody: String = "Play something and your collection starts here."
)

internal object LevyraAtelierStrings {
    private val copies = mapOf(
        "en" to AtelierCopy(
            skinBody = "A dark listening room. Sleeves stand on a shelf in real depth and every record lands on a turntable.",
            shelf = "Shelf",
            shelfEmptyTitle = "The shelf is empty",
            shelfEmptyBody = "Save songs or albums and their sleeves will appear here.",
            playCollection = "Play",
            openDetails = "Open details",
            previousSleeve = "Previous sleeve",
            nextSleeve = "Next sleeve",
            shelfPosition = "Position on the shelf",
            tracksCount = "%d tracks",
            sideA = "SIDE A · 33⅓",
            goToShelf = "Open the shelf"
        ),
        "it" to AtelierCopy(
            skinBody = "Una sala d'ascolto scura. Le custodie stanno su uno scaffale in vera profondità e ogni disco atterra su un giradischi.",
            shelf = "Scaffale",
            shelfEmptyTitle = "Lo scaffale è vuoto",
            shelfEmptyBody = "Salva brani o album e qui appariranno le loro custodie.",
            playCollection = "Riproduci",
            openDetails = "Apri i dettagli",
            previousSleeve = "Custodia precedente",
            nextSleeve = "Custodia successiva",
            shelfPosition = "Posizione sullo scaffale",
            tracksCount = "%d brani",
            sideA = "LATO A · 33⅓",
            goToShelf = "Apri lo scaffale",
            artistLabel = "Artista",
            explicit = "Esplicito",
            queueUpNext = "A seguire",
            queueMoveUp = "Sposta su",
            queueMoveDown = "Sposta giù",
            openCollection = "Collezione",
            allTools = "Tutti gli strumenti",
            featureTitle = "Selezioni",
            recentAlbums = "Album recenti",
            crateTitle = "Dalla cassa dei dischi",
            crateReroll = "Rovista ancora",
            queueHistoryEmpty = "Ancora nessun ascolto",
            retry = "Riprova",
            homeEmptyBody = "Ancora nulla sul giradischi."
        )
    )

    fun forCode(code: String): AtelierCopy = copies[LevyraLanguageCatalog.normalize(code)] ?: copies.getValue("en")
}
