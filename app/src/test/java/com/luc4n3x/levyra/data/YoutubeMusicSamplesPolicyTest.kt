package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicSamplesPolicyTest {
    @Test
    fun videoDiscoveryUsesYoutubeMusicVideoFilter() {
        assertEquals("EgWKAQIQAWoMEA4QChADEAQQCRAF", YOUTUBE_MUSIC_VIDEO_SEARCH_PARAMS)
    }

    @Test
    fun queriesPreferListeningSignalsAndStayMusicVideoSpecific() {
        val queries = youtubeMusicSampleQueries(
            seeds = listOf(track(title = "Brano", artist = "Artista ascoltato")),
            preferredArtists = listOf("Artista seguito"),
            languageCode = "it"
        )

        assertEquals("Artista seguito official music video", queries.first())
        assertTrue(queries.any { it.contains("Brano music video") })
        assertTrue(queries.contains("nuovi video musicali italiani"))
        assertFalse(queries.any { it.contains("podcast", ignoreCase = true) })
    }

    @Test
    fun languageQueriesKeepReservedSlotsEvenWithManyUserSignals() {
        val seeds = List(12) { index -> track(title = "Brano $index", artist = "Artista $index") }
        val artists = List(12) { index -> "Seguito $index" }

        val queries = youtubeMusicSampleQueries(seeds, artists, "it")

        assertEquals(8, queries.size)
        assertTrue(queries.count { it.contains("italian", ignoreCase = true) || it.contains("Italia", ignoreCase = true) } >= 3)
        assertTrue(queries.first().contains("Seguito 0"))
    }

    @Test
    fun queryGroupsAreRoundRobinInsteadOfOneSourceDominating() {
        val groups = listOf(
            listOf(track(id = "a1"), track(id = "a2")),
            listOf(track(id = "l1"), track(id = "l2")),
            listOf(track(id = "b1"), track(id = "b2"))
        )

        assertEquals(
            listOf("a1", "l1", "b1", "a2", "l2", "b2"),
            interleaveYoutubeMusicSampleResults(groups, 6).map { it.id }
        )
    }

    @Test
    fun previewStartsInsideLongMusicVideoButNotOrdinaryPlayback() {
        val sample = track(
            durationMs = 240_000L,
            source = YOUTUBE_MUSIC_SAMPLES_SOURCE
        )
        val ordinary = sample.copy(source = "YouTube Music")

        assertEquals(80_000L, youtubeMusicSamplePreviewStartMs(sample))
        assertEquals(0L, youtubeMusicSamplePreviewStartMs(ordinary))
    }

    @Test
    fun officialMusicSampleIsAcceptedBySharedSampleGate() {
        val sample = track(
            source = YOUTUBE_MUSIC_SAMPLES_SOURCE,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        assertTrue(isYoutubeShortTrack(sample))
    }

    private fun track(
        id: String = "abcdefghijk",
        title: String = "Title",
        artist: String = "Artist",
        durationMs: Long = 180_000L,
        source: String = "YouTube Music",
        videoType: String = "MUSIC_VIDEO_TYPE_OMV"
    ): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=abcdefghijk",
        thumbnailUrl = "https://levyra.test/sample.jpg",
        largeThumbnailUrl = "https://levyra.test/sample-large.jpg",
        source = source,
        moodTags = setOf("music"),
        energy = 60,
        vocal = 60,
        replayScore = 80,
        cacheScore = 70,
        accentStart = 0xFF00E5FF.toInt(),
        accentEnd = 0xFF2979FF.toInt(),
        videoType = videoType
    )
}
