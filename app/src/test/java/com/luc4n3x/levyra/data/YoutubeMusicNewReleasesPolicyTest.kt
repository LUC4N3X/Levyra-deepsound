package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ReleaseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class YoutubeMusicNewReleasesPolicyTest {
    @Test
    fun preferredAndLocallyPopularArtistsBeatUnrelatedCatalogueNoise() {
        val preferred = release("Lazza", "Nuovo disco", "preferred", ReleaseType.Album)
        val popular = release("Annalisa", "Nuovo singolo", "popular", ReleaseType.Single)
        val unrelated = release("Unknown Wave", "Real Release", "unrelated", ReleaseType.Album)

        val ranked = rankYoutubeMusicNewReleases(
            releases = listOf(unrelated, popular, preferred),
            preferredArtists = listOf("Lazza"),
            popularArtists = listOf("Annalisa"),
            currentYear = 2026,
            limit = 10
        )

        assertEquals(listOf("preferred", "popular", "unrelated"), ranked.map { it.browseId.removePrefix("MPRE") })
    }

    @Test
    fun compilationSpamAndAnonymousUnknownEntriesAreRejected() {
        val spam = release("Various Artists", "Reggae Hits Vol 2", "spam", ReleaseType.Compilation)
        val anonymous = release(
            artist = "Random Name",
            title = "Random Song",
            id = "anonymous",
            type = ReleaseType.Unknown,
            artistBrowseId = ""
        )
        val valid = release("Lazza", "Release reale", "valid", ReleaseType.Single)

        val ranked = rankYoutubeMusicNewReleases(
            releases = listOf(spam, anonymous, valid),
            preferredArtists = listOf("Lazza"),
            popularArtists = emptyList(),
            currentYear = 2026,
            limit = 10
        )

        assertEquals(listOf("MPREvalid"), ranked.map { it.browseId })
        assertFalse(ranked.any { it.title.contains("Hits Vol", ignoreCase = true) })
    }

    @Test
    fun shortArtistNamesOnlyMatchPreferencesExactly() {
        val substringNoise = release("Adele", "Album", "noise", ReleaseType.Album)
        val exactMatch = release("Ade", "Album", "exact", ReleaseType.Album)

        val ranked = rankYoutubeMusicNewReleases(
            releases = listOf(substringNoise, exactMatch),
            preferredArtists = listOf("Ade"),
            popularArtists = emptyList(),
            currentYear = 2026,
            limit = 10
        )

        assertEquals(listOf("MPREexact", "MPREnoise"), ranked.map { it.browseId })
    }

    private fun release(
        artist: String,
        title: String,
        id: String,
        type: ReleaseType,
        artistBrowseId: String = "MPLA-$id"
    ): AlbumHit = AlbumHit(
        title = title,
        artist = artist,
        year = "2026",
        thumbnailUrl = "https://levyra.test/$id.jpg",
        query = "$artist $title",
        browseId = "MPRE$id",
        artistBrowseId = artistBrowseId,
        releaseType = type
    )
}
