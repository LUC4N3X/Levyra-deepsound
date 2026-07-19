package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.AlbumRecommendationSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumRecommendationPolicyTest {
    @Test
    fun localizedAlbumLabelsAreAccepted() {
        val labels = listOf(
            "Album",
            "ÁLBUM",
            "Άλμπουμ",
            "Albüm",
            "Альбом",
            "ألبوم",
            "专辑",
            "專輯",
            "アルバム",
            "앨범",
            "एल्बम",
            "อัลบั้ม",
            "אלבום"
        )

        labels.forEach { label -> assertTrue(label, levyraIsAlbumLabel(label)) }
    }

    @Test
    fun artistSeedRejectsDifferentArtist() {
        val seed = AlbumRecommendationSeed(
            query = "Bresh album",
            artist = "Bresh",
            weight = 400
        )

        assertEquals(
            LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE,
            levyraAlbumRecommendationMatchScore(album("Vera Baddie", "ANNA"), seed)
        )
    }

    @Test
    fun exactArtistAlbumIsAccepted() {
        val seed = AlbumRecommendationSeed(
            query = "Bresh album",
            artist = "Bresh",
            weight = 400
        )

        assertTrue(levyraAlbumRecommendationMatchScore(album("Oro Blu", "Bresh"), seed) > 0)
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
            LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE,
            levyraAlbumRecommendationMatchScore(album("Greatest Hits", "Other Artist"), seed)
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
            LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE,
            levyraAlbumRecommendationMatchScore(album("La ragazza dei tuoi sogni", "Anna Tatangelo"), seed)
        )
    }

    @Test
    fun albumIdentityCollapsesEquivalentRecommendations() {
        val first = album("AMATORE", "Samurai Jay")
        val duplicate = album("  Amatore  ", "SAMURAI JAY")

        assertEquals(albumRecommendationIdentityKey(first), albumRecommendationIdentityKey(duplicate))
    }

    @Test
    fun albumDeduplicationCollapsesSameReleaseWithExpandedArtistCredits() {
        val first = album("AMATORE", "Samurai Jay").copy(
            thumbnailUrl = "https://lh3.googleusercontent.com/cover=w544-h544"
        )
        val duplicate = album("AMATORE", "Samurai Jay, Vito Salamanca").copy(
            thumbnailUrl = "https://lh3.googleusercontent.com/cover=w1200-h1200"
        )

        assertEquals(
            albumRecommendationDeduplicationKey(first),
            albumRecommendationDeduplicationKey(duplicate)
        )
    }

    @Test
    fun albumDeduplicationIgnoresDifferentUpcAndBrowseIdsForSameVisibleRelease() {
        val first = album("AMATORE", "Samurai Jay").copy(
            upc = "0602475840112",
            browseId = "MPREb_first"
        )
        val duplicate = album("AMATORE", "Samurai Jay").copy(
            upc = "0602475840999",
            browseId = "MPREb_second"
        )

        assertEquals(
            albumRecommendationDeduplicationKey(first),
            albumRecommendationDeduplicationKey(duplicate)
        )
    }

    @Test
    fun albumDeduplicationCollapsesEditionSuffixesForTheSameArtist() {
        val standard = album("AMATORE", "Samurai Jay")
        val deluxe = album("AMATORE Deluxe Edition", "Samurai Jay")

        assertEquals(
            albumRecommendationDeduplicationKey(standard),
            albumRecommendationDeduplicationKey(deluxe)
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
