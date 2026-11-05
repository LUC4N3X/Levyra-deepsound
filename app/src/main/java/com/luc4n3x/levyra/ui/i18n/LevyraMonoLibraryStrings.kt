package com.luc4n3x.levyra.ui.i18n

internal data class MonoLibraryCopy(
    val yourLibrary: String,
    val deckOverline: String,
    val deckTitle: String,
    val recentOverline: String,
    val recentlyAdded: String,
    val recentSubtitle: String,
    val collectionOne: String,
    val collectionMany: String,
    val playlistKind: String,
    val albumKind: String,
    val artistKind: String,
    val songKind: String,
    val manage: String,
    val options: String,
    val allTools: String,
    val scrubber: String
) {
    fun collections(count: Int, formatted: String): String = "$formatted ${if (count == 1) collectionOne else collectionMany}"
}

internal object LevyraMonoLibraryStrings {
    private val copies = mapOf(
        "en" to MonoLibraryCopy(
            yourLibrary = "Your library",
            deckOverline = "Flip through the deck",
            deckTitle = "Your collections",
            recentOverline = "Latest arrivals",
            recentlyAdded = "Recently added",
            recentSubtitle = "Played lately",
            collectionOne = "collection",
            collectionMany = "collections",
            playlistKind = "Playlist",
            albumKind = "Album",
            artistKind = "Artist",
            songKind = "Song",
            manage = "Manage",
            options = "Library options",
            allTools = "All tools",
            scrubber = "Jump to letter"
        ),
        "it" to MonoLibraryCopy(
            yourLibrary = "La tua libreria",
            deckOverline = "Sfoglia il mazzo",
            deckTitle = "Le tue raccolte",
            recentOverline = "Ultimi arrivi",
            recentlyAdded = "Aggiunti di recente",
            recentSubtitle = "Ascoltati di recente",
            collectionOne = "raccolta",
            collectionMany = "raccolte",
            playlistKind = "Playlist",
            albumKind = "Album",
            artistKind = "Artista",
            songKind = "Brano",
            manage = "Gestisci",
            options = "Opzioni libreria",
            allTools = "Tutti gli strumenti",
            scrubber = "Vai alla lettera"
        )
    )

    fun forCode(code: String): MonoLibraryCopy = localizedValueOrEnglish(copies, code)
}
