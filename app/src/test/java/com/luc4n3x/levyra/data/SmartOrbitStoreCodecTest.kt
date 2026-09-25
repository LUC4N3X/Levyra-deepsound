package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.SmartOrbitCandidate
import com.luc4n3x.levyra.domain.SmartOrbitPool
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartOrbitStoreCodecTest {

    private fun track(id: String) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist",
        album = "",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )

    @Test
    fun poolRoundTripsThroughJson() {
        val pool = SmartOrbitPool(
            listOf(SmartOrbitCandidate(track("x"), listOf("a", "b"), 42L))
        )

        val decoded = decodeSmartOrbitPool(encodeSmartOrbitPool(pool))

        assertEquals(listOf("x"), decoded.candidates.map { it.track.id })
        assertEquals(listOf("a", "b"), decoded.candidates.single().seedKeys)
        assertEquals(42L, decoded.candidates.single().lastSeenAt)
    }

    @Test
    fun unknownVersionAndMalformedEntriesAreIgnored() {
        assertTrue(decodeSmartOrbitPool("""{"version":99,"candidates":[]}""").isEmpty)
        val decoded = decodeSmartOrbitPool(
            """{"version":1,"candidates":[{"track":"broken","seeds":["a"]},{"track":"x"}]}"""
        )
        assertTrue(decoded.isEmpty)
    }
}
