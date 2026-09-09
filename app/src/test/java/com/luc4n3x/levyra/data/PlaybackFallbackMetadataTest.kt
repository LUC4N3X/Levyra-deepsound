package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackFallbackMetadataTest {
    @Test
    fun recognizesOnlyKnownMisattributedArtistTitleSignature() {
        assertTrue(isKnownMisattributedPlaybackMetadata("neptune.", "SOTTOGONNA"))
        assertTrue(isKnownMisattributedPlaybackMetadata("NEPTUNE", "sottogonna!"))
        assertFalse(isKnownMisattributedPlaybackMetadata("NEPTUNE", "ALTALENA"))
        assertFalse(isKnownMisattributedPlaybackMetadata("BLANCO", "SOTTOGONNA"))
    }

    @Test
    fun canonicalMetadataMatchRejectsKnownMisattributionAndChoosesOfficialRecording() {
        val original = track(
            id = "chart-source",
            title = "SOTTOGONNA",
            artist = "neptune.",
            durationMs = 175_000L
        )
        val leaked = track(
            id = "leaked",
            title = "SOTTOGONNA",
            artist = "neptune.",
            durationMs = 175_000L
        )
        val official = track(
            id = "official",
            title = "SOTTOGONNA",
            artist = "BLANCO",
            durationMs = 175_000L,
            album = "SOTTOGONNA",
            isrc = "ITUM72600001",
            metadataConfidence = 95
        )

        val match = bestCanonicalPlaybackMetadataMatch(original, listOf(leaked, official))

        assertEquals("official", match?.id)
        assertEquals("BLANCO", match?.artist)
    }

    @Test
    fun canonicalMetadataMatchRejectsDifferentRecordingDuration() {
        val original = track(
            id = "chart-source",
            title = "ALTALENA",
            artist = "neptune.",
            durationMs = 190_000L
        )
        val unrelated = track(
            id = "other",
            title = "ALTALENA",
            artist = "Another Artist",
            durationMs = 260_000L
        )

        assertNull(bestCanonicalPlaybackMetadataMatch(original, listOf(unrelated)))
    }

    @Test
    fun misattributedArtistQueriesTryTitleOnlyOfficialResultsFirst() {
        val queries = playbackAlternativeSearchQueries(
            track(
                id = "chart-source",
                title = "SOTTOGONNA",
                artist = "neptune.",
                durationMs = 175_000L
            )
        )

        assertEquals("SOTTOGONNA official audio", queries.first())
        assertTrue("SOTTOGONNA" in queries)
    }

    @Test
    fun canonicalFallbackArtistAdoptsVerifiedOfficialArtist() {
        val original = track(
            id = "chart-source",
            title = "SOTTOGONNA",
            artist = "neptune.",
            durationMs = 175_000L
        )
        val candidate = track(
            id = "official",
            title = "SOTTOGONNA",
            artist = "BLANCO",
            durationMs = 175_000L
        )

        assertEquals(
            "BLANCO",
            canonicalPlaybackFallbackArtist(original, candidate, candidate)
        )
    }

    private fun track(
        id: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String = "",
        isrc: String = "",
        metadataConfidence: Int = 0
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0,
        isrc = isrc,
        metadataConfidence = metadataConfidence
    )
}
