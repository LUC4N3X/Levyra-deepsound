package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerifiedCounterpartVideoTrackTest {
    @Test
    fun verifiedPairUsesExactCounterpartWithoutChangingAudioIdentity() {
        val track = track(
            audioVideoId = "Audio123456",
            counterpartVideoId = "fcnDmrtj6Sk",
        )

        val selected = verifiedCounterpartVideoTrack(track)

        assertEquals("https://www.youtube.com/watch?v=fcnDmrtj6Sk", selected?.videoUrl)
        assertEquals("Audio123456", selected?.audioVideoId)
        assertEquals("fcnDmrtj6Sk", selected?.counterpartVideoId)
    }

    @Test
    fun missingInvalidOrSameCounterpartKeepsHeuristicFallbackAvailable() {
        assertNull(verifiedCounterpartVideoTrack(track(audioVideoId = "", counterpartVideoId = "fcnDmrtj6Sk")))
        assertNull(verifiedCounterpartVideoTrack(track(audioVideoId = "Audio123456", counterpartVideoId = "invalid")))
        assertNull(verifiedCounterpartVideoTrack(track(audioVideoId = "Audio123456", counterpartVideoId = "Audio123456")))
    }

    private fun track(audioVideoId: String, counterpartVideoId: String): Track = Track(
        id = "chart-test",
        title = "Dai Dai",
        artist = "Shakira, Burna Boy",
        album = "Dai Dai",
        durationMs = 224_000L,
        streamUrl = "",
        videoUrl = audioVideoId.takeIf(String::isNotBlank)
            ?.let { "https://www.youtube.com/watch?v=$it" }
            .orEmpty(),
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "Levyra Editorial",
        moodTags = setOf("chart"),
        energy = 70,
        vocal = 55,
        replayScore = 95,
        cacheScore = 88,
        accentStart = 0,
        accentEnd = 0,
        counterpartVideoId = counterpartVideoId,
        videoType = "MUSIC_VIDEO_TYPE_ATV",
        audioVideoId = audioVideoId,
        metadataProvider = "Levyra Editorial + YouTube Music",
        metadataConfidence = 100,
    )
}
