package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshCurrentsPolicyTest {
    @Test
    fun officialEditorialFeedWinsOverFallback() {
        val editorial = track("editorial", "Official release", "Artist")
        val fallback = track("fallback", "Chart fallback", "Artist")
        assertEquals(listOf(editorial), selectFreshCurrentTracks(listOf(editorial), listOf(fallback), 12))
    }

    @Test
    fun samplesCanNeverLeakIntoFreshCurrents() {
        val short = track("short", "Vertical clip", "Artist", source = YOUTUBE_SHORTS_SOURCE, videoType = "SHORTS")
        val clean = track("clean", "Clean song", "Artist")
        val result = selectFreshCurrentTracks(listOf(short), listOf(clean), 12)
        assertEquals(listOf(clean), result)
        assertFalse(result.any(::isYoutubeShortTrack))
    }

    @Test
    fun worldBlendRotatesMarketsAndSkipsTheHomeMarket() {
        val releases = mapOf(
            "IT" to listOf(track("it-1", "Home release", "Home artist")),
            "US" to listOf(track("us-1", "US one", "US artist"), track("us-2", "US two", "US artist")),
            "GB" to listOf(track("gb-1", "GB one", "GB artist"))
        )

        val blended = blendWorldFreshTracks("it", releases, emptyList(), 10)

        assertEquals(listOf("us-1", "gb-1", "us-2"), blended.map { it.id })
    }

    @Test
    fun worldBlendNeverRepeatsWhatTheLocalFeedAlreadyShows() {
        val shared = track("us-1", "Shared song", "Shared artist")
        val releases = mapOf(
            "US" to listOf(shared, track("us-2", "US two", "US artist")),
            "FR" to listOf(track("fr-1", "Shared song", "Shared artist"))
        )

        val blended = blendWorldFreshTracks(
            homeMarket = "IT",
            releasesByMarket = releases,
            localTracks = listOf(track("local", "Shared song", "Shared artist")),
            limit = 10
        )

        assertEquals(listOf("us-2"), blended.map { it.id })
    }

    @Test
    fun worldBlendStaysBoundedAndRejectsUnusableEntries() {
        val releases = mapOf(
            "US" to List(12) { index -> track("us-$index", "US $index", "US artist") },
            "JP" to listOf(track("jp-short", "Vertical clip", "JP artist", source = YOUTUBE_SHORTS_SOURCE, videoType = "SHORTS"))
        )

        val blended = blendWorldFreshTracks("IT", releases, emptyList(), 4)

        assertEquals(4, blended.size)
        assertTrue(blended.none { it.id == "jp-short" })
        assertTrue(blendWorldFreshTracks("IT", emptyMap(), emptyList(), 4).isEmpty())
    }

    private fun track(id: String, title: String, artist: String, source: String = "Editorial", videoType: String = ""): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = title,
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=abcdefghijk",
        thumbnailUrl = "https://example.test/$id.jpg",
        largeThumbnailUrl = "https://example.test/$id-large.jpg",
        source = source,
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0,
        accentEnd = 0,
        videoType = videoType
    )
}
