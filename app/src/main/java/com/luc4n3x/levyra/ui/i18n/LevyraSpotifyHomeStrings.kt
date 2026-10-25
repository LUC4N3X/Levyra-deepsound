package com.luc4n3x.levyra.ui.i18n

internal data class SpotifyHomeCopy(
    val popularRadio: String,
    val popularAlbums: String,
    val popularArtists: String,
    val latestReleases: String,
    val betterRecommendations: String,
    val chooseDescription: String,
    val chooseArtists: String
)

internal object LevyraSpotifyHomeStrings {
    fun forCode(code: String): SpotifyHomeCopy {
        val strings = LevyraStrings.forCode(code)
        if (strings.code == "it") return SpotifyHomeCopy(
            "Stazioni radio più popolari", "Album e singoli popolari", "Artisti più popolari", "Ultime uscite",
            "Ricevi consigli più accurati", "Facci sapere quali sono i tuoi artisti preferiti e ti aiuteremo a scoprire altra musica che ti piacerà.", "Scegli gli artisti"
        )
        return SpotifyHomeCopy(
            strings.continuousRadio, strings.albumsAndSingles, strings.trendingArtists, strings.newReleases,
            strings.smartMusicProfile, strings.followedArtistsSubtitle, strings.artists
        )
    }
}
