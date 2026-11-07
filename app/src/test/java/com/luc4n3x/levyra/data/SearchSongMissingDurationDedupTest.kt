package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchSongMissingDurationDedupTest {

    @Test
    fun `explicit and unbadged editions with matching metadata stay separate`() {
        val clean = track("clean", "Song", "Artist", 180_000L)
        val explicit = clean.copy(id = "explicit", explicit = true)

        val songs = deduplicateSearchSongs(listOf(clean, explicit))

        assertEquals(listOf("clean", "explicit"), songs.map { it.id })
        assertEquals(listOf(false, true), songs.map { it.explicit })
    }

    @Test
    fun `missing duration does not merge explicit and unbadged editions`() {
        val clean = track("clean", "Song", "Artist", 180_000L)
        val explicit = clean.copy(id = "explicit", explicit = true, durationMs = 0L)

        assertEquals(2, deduplicateSearchSongs(listOf(clean, explicit)).size)
    }

    @Test
    fun `explicit top result does not hide an unbadged edition with the same isrc`() {
        val clean = track("clean", "Song", "Artist", 180_000L).copy(isrc = "US1234567890")
        val explicit = clean.copy(id = "explicit", explicit = true)

        assertEquals(listOf(clean), filterSearchSongsExcludingTopResult(listOf(clean), listOf(explicit)))
    }

    @Test
    fun `the same video still merges when only one copy has the explicit badge`() {
        val clean = track("same-video", "Song", "Artist", 180_000L)
        val explicit = clean.copy(explicit = true)

        assertEquals(listOf(explicit), deduplicateSearchSongs(listOf(clean, explicit)))
    }

    @Test
    fun `same song merges when one result has duration and the other has views`() {
        val durationResult = track(
            id = "duration-result",
            title = "Da Dio",
            artist = "Bresh",
            durationMs = 173_000L
        )
        val viewsResult = track(
            id = "views-result",
            title = "Da Dio",
            artist = "Bresh",
            durationMs = 0L,
            youtubeViewCount = 5_500_000L
        )

        val merged = deduplicateSearchSongs(listOf(durationResult, viewsResult))

        assertEquals(1, merged.size)
        assertEquals(173_000L, merged.single().durationMs)
        assertEquals(5_500_000L, merged.single().youtubeViewCount)
    }

    @Test
    fun `dedup keeps first search result identity while enriching missing metadata`() {
        val viewsResult = track(
            id = "views-result",
            title = "Da Dio",
            artist = "Bresh",
            durationMs = 0L,
            youtubeViewCount = 5_500_000L
        )
        val durationResult = track(
            id = "duration-result",
            title = "Da Dio",
            artist = "Bresh",
            durationMs = 173_000L
        )

        val merged = deduplicateSearchSongs(listOf(viewsResult, durationResult))

        assertEquals(1, merged.size)
        assertEquals("views-result", merged.single().id)
        assertEquals(173_000L, merged.single().durationMs)
        assertEquals(5_500_000L, merged.single().youtubeViewCount)
    }

    @Test
    fun `top result replaces partial duplicate with next unique song`() {
        val hero = track(
            id = "views-result",
            title = "Da Dio",
            artist = "Bresh",
            durationMs = 0L,
            youtubeViewCount = 5_500_000L
        )
        val duplicate = track(
            id = "duration-result",
            title = "Da Dio",
            artist = "Bresh",
            durationMs = 173_000L
        )
        val nextSong = track(
            id = "next-song",
            title = "Guasto d'amore",
            artist = "Bresh",
            durationMs = 186_000L,
            youtubeViewCount = 18_000_000L
        )
        val thirdSong = track(
            id = "third-song",
            title = "Andrea",
            artist = "Bresh",
            durationMs = 194_000L,
            youtubeViewCount = 7_000_000L
        )

        val topResults = selectSearchTopResultTracks(
            topTrack = hero,
            songs = listOf(duplicate, nextSong, thirdSong),
            limit = 3
        )

        assertEquals(listOf("views-result", "next-song", "third-song"), topResults.map(Track::id))
        assertEquals(173_000L, topResults.first().durationMs)
        assertEquals(5_500_000L, topResults.first().youtubeViewCount)
    }

    private fun track(
        id: String,
        title: String,
        artist: String,
        durationMs: Long,
        youtubeViewCount: Long = -1L
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = "",
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        youtubeViewCount = youtubeViewCount
    )
}
