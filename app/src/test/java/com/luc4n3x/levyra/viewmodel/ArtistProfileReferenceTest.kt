package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.ArtistProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistProfileReferenceTest {

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
    fun rejectedBrowseIdStillResolvesTheRequestedIdentityByName() = runBlocking {
        val canonical = profile("UC_CANONICAL", "Requested Artist")

        val resolved = resolveArtistProfileReference(
            browseId = "UC_STALE",
            name = "Requested Artist",
            isActive = { true },
            profileByBrowseId = { _, _ -> null },
            profileByName = { name -> canonical.takeIf { it.name == name } }
        )

        assertEquals("UC_CANONICAL", resolved?.browseId)
    }

    @Test
    fun usableBrowseIdIsNotReplacedByTheNameLookup() = runBlocking {
        var nameLookups = 0
        val direct = profile("UC_DIRECT", "Requested Artist")

        val resolved = resolveArtistProfileReference(
            browseId = "UC_DIRECT",
            name = "Requested Artist",
            isActive = { true },
            profileByBrowseId = { _, _ -> direct },
            profileByName = {
                nameLookups++
                profile("UC_OTHER", "Requested Artist")
            }
        )

        assertEquals("UC_DIRECT", resolved?.browseId)
        assertEquals(0, nameLookups)
    }

    @Test
    fun cancelledRequestDoesNotStartTheNameLookup() = runBlocking {
        var nameLookups = 0

        val resolved = resolveArtistProfileReference(
            browseId = "UC_STALE",
            name = "Requested Artist",
            isActive = { false },
            profileByBrowseId = { _, _ -> null },
            profileByName = {
                nameLookups++
                profile("UC_CANONICAL", "Requested Artist")
            }
        )

        assertNull(resolved)
        assertEquals(0, nameLookups)
    }
}
