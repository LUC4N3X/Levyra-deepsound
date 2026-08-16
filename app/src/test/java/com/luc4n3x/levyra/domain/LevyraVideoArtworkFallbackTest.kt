package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraVideoArtworkFallbackTest {
    @Test
    fun `counterpart video id provides artwork when song artwork is missing`() {
        val track = testTrack(id = "catalog-entry", counterpartVideoId = "AbCdEfGhI12")
        assertEquals(
  "https://i.ytimg.com/vi/AbCdEfGhI12/hqdefault.jpg",
  LevyraPersonalOrbit.youtubeFallbackArtwork(track)
        )
    }

    @Test
    fun `video artwork fallback is independent from localized display text`() {
        val italian = testTrack(
  id = "catalog-entry",
  counterpartVideoId = "AbCdEfGhI12",
  title = "Dopo il tramonto",
  artist = "Artista"
        )
        val japanese = italian.copy(title = "夜のあと", artist = "アーティスト")
        assertEquals(
  LevyraPersonalOrbit.youtubeFallbackArtwork(italian),
  LevyraPersonalOrbit.youtubeFallbackArtwork(japanese)
        )
    }

    private fun testTrack(
        id: String,
        counterpartVideoId: String,
        title: String = "Video",
        artist: String = "Artist"
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = "",
        durationMs = 0L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        counterpartVideoId = counterpartVideoId
    )
}
