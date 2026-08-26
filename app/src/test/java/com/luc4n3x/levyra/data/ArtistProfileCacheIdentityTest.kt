package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.ArtistProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistProfileCacheIdentityTest {

    private fun profile(browseId: String, name: String) = ArtistProfile(
        browseId = browseId,
        name = name,
        subscribers = "",
        monthlyListeners = "",
        thumbnailUrl = "",
        bannerUrl = "",
        topSongs = emptyList(),
        albums = emptyList(),
        singles = emptyList(),
        accentStart = 0,
        accentEnd = 0
    )

    @Test
    fun profileWhoseNameDoesNotMatchTheRequestIsNeverAccepted() {
        val fetched = profile("UC_ONE", "Artist One")

        assertFalse(artistProfileMatchesRequest(fetched, "UC_ONE", "Artist Two"))
        assertTrue(artistProfileMatchesRequest(fetched, "UC_ONE", "Artist One"))
    }

    @Test
    fun cachedProfileOfOneArtistIsNeverServedForAnotherArtist() {
        val cached = profile("UC_ONE", "Artist One")

        assertFalse(artistProfileMatchesRequest(cached, "UC_TWO", "Artist Two"))
        assertFalse(artistProfileMatchesRequest(cached, "UC_ONE", "Artist Two"))
        assertFalse(artistProfileMatchesRequest(cached, "UC_TWO", "Artist One"))
    }

    @Test
    fun cachedProfileStillAnswersItsOwnIdentityIncludingACollaborativeCredit() {
        val cached = profile("UC_ONE", "Artist One")

        assertTrue(artistProfileMatchesRequest(cached, "UC_ONE", "Artist One"))
        assertTrue(artistProfileMatchesRequest(cached, "uc_one", "Artist One, Artist Two"))
        assertTrue(artistProfileMatchesRequest(cached, "", "Artist One"))
    }
}
