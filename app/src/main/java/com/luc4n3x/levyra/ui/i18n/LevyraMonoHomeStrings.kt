package com.luc4n3x.levyra.ui.i18n

internal data class MonoHomeCopy(
    val all: String,
    val mood: String,
    val startHere: String,
    val trendingOverline: String,
    val popularNow: String,
    val rotationOverline: String,
    val rotation: String,
    val againOverline: String,
    val listenAgain: String,
    val freshOverline: String,
    val newAlbums: String,
    val forYouOverline: String,
    val artistChartOverline: String,
    val topArtists: String,
    val rankLabel: String,
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
            startHere = "Start here",
            trendingOverline = "Trending",
            popularNow = "Popular now",
            rotationOverline = "On repeat",
            rotation = "Your rotation",
            againOverline = "History",
            listenAgain = "Listen again",
            freshOverline = "Just out",
            newAlbums = "New albums & singles",
            forYouOverline = "For you",
            artistChartOverline = "Artist chart",
            topArtists = "Top artists",
            rankLabel = "Rank %d",
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
            startHere = "Inizia da qui",
            trendingOverline = "In tendenza",
            popularNow = "Popolari ora",
            rotationOverline = "In loop",
            rotation = "La tua rotazione",
            againOverline = "Cronologia",
            listenAgain = "Ascolta di nuovo",
            freshOverline = "Appena usciti",
            newAlbums = "Nuovi album e singoli",
            forYouOverline = "Per te",
            artistChartOverline = "Classifica artisti",
            topArtists = "Top artisti",
            rankLabel = "Posizione %d",
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
