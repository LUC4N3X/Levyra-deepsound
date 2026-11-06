package com.luc4n3x.levyra.ui.i18n

internal data class MonoHomeCopy(
    val all: String,
    val mood: String,
    val startHere: String,
    val trendingOverline: String,
    val rotationOverline: String,
    val rotation: String,
    val mixedForYou: String,
    val newVideos: String,
    val againOverline: String,
    val listenAgain: String,
    val freshOverline: String,
    val newAlbums: String,
    val forYouOverline: String,
    val tracksCount: String,
    val emptyBody: String,
    val retry: String,
    val momentsOverline: String,
    val moodsTitle: String,
    val genresOverline: String,
    val genresTitle: String,
    val chartsOverline: String,
    val searchPrompt: String
)

internal object LevyraMonoHomeStrings {
    private val copies = mapOf(
        "en" to MonoHomeCopy(
            all = "All",
            mood = "Mood",
            startHere = "Tuned to you",
            trendingOverline = "Trending",
            rotationOverline = "Hand-picked",
            rotation = "Your rotation",
            mixedForYou = "Curated playlists",
            newVideos = "New music videos",
            againOverline = "History",
            listenAgain = "Listen again",
            freshOverline = "Just out",
            newAlbums = "New albums & singles",
            forYouOverline = "For you",
            tracksCount = "%d tracks",
            emptyBody = "Nothing here yet. Play something or pull to refresh.",
            retry = "Try again",
            momentsOverline = "For every moment",
            moodsTitle = "Moods & moments",
            genresOverline = "By sound",
            genresTitle = "Genres",
            chartsOverline = "Charts",
            searchPrompt = "Search songs, albums, artists…"
        ),
        "it" to MonoHomeCopy(
            all = "Tutto",
            mood = "Mood",
            startHere = "Su misura",
            trendingOverline = "In tendenza",
            rotationOverline = "Scelte per te",
            rotation = "La tua rotazione",
            mixedForYou = "Playlist d'autore",
            newVideos = "Nuovi video musicali",
            againOverline = "Cronologia",
            listenAgain = "Ascolta di nuovo",
            freshOverline = "Appena usciti",
            newAlbums = "Nuovi album e singoli",
            forYouOverline = "Per te",
            tracksCount = "%d brani",
            emptyBody = "Ancora niente qui. Riproduci qualcosa o trascina per aggiornare.",
            retry = "Riprova",
            momentsOverline = "Per ogni momento",
            moodsTitle = "Mood e momenti",
            genresOverline = "Per suono",
            genresTitle = "Generi",
            chartsOverline = "Classifiche",
            searchPrompt = "Cerca brani, album, artisti…"
        )
    )

    fun forCode(code: String): MonoHomeCopy = localizedValueOrEnglish(copies, code)
}
