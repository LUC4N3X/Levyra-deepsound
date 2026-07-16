package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.AlbumRecommendationSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumRecommendationPolicyTest {
    @Test
    fun artistSeedRejectsDifferentArtist() {
        val seed = AlbumRecommendationSeed(
            query = "Bresh album",
            artist = "Bresh",
            weight = 400
        )

        assertEquals(
            REJECTED_ALBUM_RECOMMENDATION_SCORE,
            albumRecommendationMatchScore(album("Vera Baddie", "ANNA"), seed)
        )
    }

    @Test
    fun exactArtistAlbumIsAccepted() {
        val seed = AlbumRecommendationSeed(
            query = "Bresh album",
            artist = "Bresh",
            weight = 400
        )

        assertTrue(albumRecommendationMatchScore(album("Oro Blu", "Bresh"), seed) > 0)
    }

    @Test
    fun commonAlbumTitleCannotOverrideArtistMismatch() {
        val seed = AlbumRecommendationSeed(
            query = "Greatest Hits Bresh album",
            artist = "Bresh",
            album = "Greatest Hits",
            weight = 500
        )

        assertEquals(
            REJECTED_ALBUM_RECOMMENDATION_SCORE,
            albumRecommendationMatchScore(album("Greatest Hits", "Other Artist"), seed)
        )
    }

    @Test
    fun singleWordArtistDoesNotMatchLongerUnrelatedName() {
        val seed = AlbumRecommendationSeed(
            query = "ANNA album",
            artist = "ANNA",
            weight = 400
        )

        assertEquals(
            REJECTED_ALBUM_RECOMMENDATION_SCORE,
            albumRecommendationMatchScore(album("La ragazza dei tuoi sogni", "Anna Tatangelo"), seed)
        )
    }

    private fun album(title: String, artist: String): AlbumHit = AlbumHit(
        title = title,
        artist = artist,
        year = "",
        thumbnailUrl = "https://example.test/cover.jpg",
        query = "$title $artist",
        browseId = "MPREb_test"
    )
}
