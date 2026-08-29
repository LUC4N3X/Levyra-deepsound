package com.luc4n3x.levyra.feature.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppleArtistMotionSelectionTest {
    private val video = AppleEditorialVideo("https://example.com/motion.m3u8", 900, 1200)

    @Test
    fun oneUniqueArtistMatchIsSelected() {
        val match = AppleArtistMotionMatch("artist-1", "Artist", video)

        assertEquals(match, selectUnambiguousAppleArtistMotion(listOf(match, match)))
    }

    @Test
    fun homonymousArtistMatchesAreRejected() {
        val matches = listOf(
            AppleArtistMotionMatch("artist-1", "Artist", video),
            AppleArtistMotionMatch("artist-2", "Artist", video.copy(url = "https://example.com/other.m3u8"))
        )

        assertNull(selectUnambiguousAppleArtistMotion(matches))
    }

    @Test
    fun artistMotionChecksUsBeforeLocaleStorefront() {
        assertEquals(listOf("us", "it"), appleArtistMotionStorefronts("IT"))
        assertEquals(listOf("us"), appleArtistMotionStorefronts("us"))
    }
}
