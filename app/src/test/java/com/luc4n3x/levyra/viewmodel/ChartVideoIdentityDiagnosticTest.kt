package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A chart-origin track carries no authoritative pairing (no counterpart, no browse ids), so these
 * cases pin the two halves of native-video selection for it: a confirmed official video must win,
 * and an unverified identity must never be promoted to video mode.
 */
class ChartVideoIdentitySelectionTest {

    private fun track(
        id: String,
        title: String,
        artist: String,
        videoUrl: String = "",
        videoType: String = "",
        artistBrowseIds: List<String> = emptyList()
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = "",
        durationMs = 188_000L,
        streamUrl = "",
        videoUrl = videoUrl,
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
        videoType = videoType,
        artistBrowseIds = artistBrowseIds
    )

    private fun chartTarget() = track(
        id = "aaaaaaaaaaa",
        title = "OSSESSIONE",
        artist = "Samurai Jay, Vito Salamanca",
        videoUrl = "https://www.youtube.com/watch?v=aaaaaaaaaaa",
        videoType = "MUSIC_VIDEO_TYPE_ATV"
    )

    private fun officialVideo(title: String, artist: String) = track(
        id = "kqTEc17L2Os",
        title = title,
        artist = artist,
        videoUrl = "https://www.youtube.com/watch?v=kqTEc17L2Os",
        videoType = "MUSIC_VIDEO_TYPE_OMV"
    )

    @Test
    fun officialVideoWinsForAMultiArtistChartTitle() {
        val target = chartTarget()
        val variants = listOf(
            officialVideo("OSSESSIONE", "Samurai Jay"),
            officialVideo("SAMURAI JAY, VITO SALAMANCA - OSSESSIONE (Official Video)", "Samurai Jay"),
            officialVideo("OSSESSIONE (Official Video)", "Samurai Jay, Vito Salamanca")
        )

        variants.forEach { candidate ->
            assertTrue(
                "official video rejected as incompatible: ${candidate.title}",
                isPlaybackCandidateCompatible(target, candidate)
            )
            assertEquals(
                "official video not selected: ${candidate.title}",
                "kqTEc17L2Os",
                selectPreferredVideoPlaybackCandidate(target, listOf(candidate))?.let(::videoCandidateId)
            )
        }
    }

    @Test
    fun artTrackNeverWinsOverTheOfficialVideo() {
        val target = chartTarget()
        val artTrack = track(
            id = "bbbbbbbbbbb",
            title = "OSSESSIONE",
            artist = "Samurai Jay",
            videoUrl = "https://www.youtube.com/watch?v=bbbbbbbbbbb",
            videoType = "MUSIC_VIDEO_TYPE_ATV"
        )

        val selected = selectPreferredVideoPlaybackCandidate(
            target,
            listOf(artTrack, officialVideo("OSSESSIONE", "Samurai Jay"))
        )

        assertEquals("kqTEc17L2Os", selected?.let(::videoCandidateId))
    }

    @Test
    fun unverifiedChartIdentityStaysOutOfVideoMode() {
        assertNull(youtubePlayableTrack(chartTarget(), preferVideo = true))
    }
}
