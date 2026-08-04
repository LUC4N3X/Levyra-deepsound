package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ReleaseType
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeAlbumDeduplicationTest {
    @Test
    fun localizedArtistConnectorAndProviderSeparatorCollapseToOneAlbum() {
        val localized = album(
            title = "AMATORE",
            artist = "Samurai Jay e Vito Salamanca",
            browseId = "MPREb_localized"
        )
        val providerVariant = album(
            title = "AMATORE",
            artist = "Samurai Jay, Vito Salamanca",
            browseId = "MPREb_provider"
        )

        assertEquals(listOf(localized), deduplicateHomeAlbums(listOf(localized, providerVariant)))
    }

    @Test
    fun differentReleasesRemainVisible() {
        val first = album("AMATORE", "Samurai Jay e Vito Salamanca", "MPREb_first")
        val second = album("LACRIME", "Samurai Jay e Vito Salamanca", "MPREb_second")

        assertEquals(listOf(first, second), deduplicateHomeAlbums(listOf(first, second)))
    }

    private fun album(title: String, artist: String, browseId: String): AlbumHit = AlbumHit(
        title = title,
        artist = artist,
        year = "2026",
        thumbnailUrl = "https://example.test/$browseId.jpg",
        query = "$title $artist",
        browseId = browseId,
        releaseType = ReleaseType.Album
    )
}
