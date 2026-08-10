package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfficialVideoCandidateTest {
    @Test
    fun prefersOfficialCounterpartForAudioTrack() {
        val audio = watchTrack(
            videoId = "audio123456",
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            counterpart = watchTrack("video123456", "MUSIC_VIDEO_TYPE_OMV")
        )

        val candidate = officialYoutubeMusicVideoCandidate(
            "audio123456",
            YoutubeMusicWatchPlaylist(listOf(audio), "", "", "", "")
        )

        assertEquals("video123456", candidate?.videoId)
    }

    @Test
    fun keepsPrimaryWhenItIsAlreadyOfficial() {
        val official = watchTrack(
            videoId = "video123456",
            videoType = "MUSIC_VIDEO_TYPE_OMV",
            counterpart = watchTrack("audio123456", "MUSIC_VIDEO_TYPE_ATV")
        )

        val candidate = officialYoutubeMusicVideoCandidate(
            "video123456",
            YoutubeMusicWatchPlaylist(listOf(official), "", "", "", "")
        )

        assertEquals("video123456", candidate?.videoId)
    }

    @Test
    fun artTrackStillLooksForOfficialVideo() {
        assertEquals(true, shouldLookupOfficialVideo("MUSIC_VIDEO_TYPE_ATV"))
    }

    @Test
    fun visualVideoTypesDoNotGetRewritten() {
        assertEquals(false, shouldLookupOfficialVideo("MUSIC_VIDEO_TYPE_OMV"))
        assertEquals(false, shouldLookupOfficialVideo("MUSIC_VIDEO_TYPE_UGC"))
    }

    @Test
    fun rejectsUserGeneratedCounterpartAsOfficial() {
        val audio = watchTrack(
            videoId = "audio123456",
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            counterpart = watchTrack("upload12345", "MUSIC_VIDEO_TYPE_UGC")
        )

        val candidate = officialYoutubeMusicVideoCandidate(
            "audio123456",
            YoutubeMusicWatchPlaylist(listOf(audio), "", "", "", "")
        )

        assertNull(candidate)
    }

    private fun watchTrack(
        videoId: String,
        videoType: String,
        counterpart: YoutubeMusicWatchTrack? = null
    ) = YoutubeMusicWatchTrack(
        videoId = videoId,
        title = "Song",
        artists = emptyList(),
        albumTitle = "Album",
        albumBrowseId = "",
        durationMs = 180_000L,
        thumbnailUrl = "",
        videoType = videoType,
        explicit = false,
        counterpart = counterpart
    )
}
