package com.luc4n3x.levyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicWatchPairingTest {
    @Test
    fun requestedAudioWithCounterpartCanStopBeforeContinuation() {
        val playlist = playlist(
            track(
                videoId = "lFQdcPTTzSg",
                videoType = "MUSIC_VIDEO_TYPE_ATV",
                counterpart = track(
                    videoId = "fcnDmrtj6Sk",
                    videoType = "MUSIC_VIDEO_TYPE_OMV"
                )
            )
        )

        assertTrue(playlist.hasRequestedCounterpart("lFQdcPTTzSg"))
    }

    @Test
    fun requestedAudioWithoutCounterpartStillAllowsContinuation() {
        val playlist = playlist(
            track(
                videoId = "lFQdcPTTzSg",
                videoType = "MUSIC_VIDEO_TYPE_ATV"
            )
        )

        assertFalse(playlist.hasRequestedCounterpart("lFQdcPTTzSg"))
    }

    @Test
    fun requestedIdCanBeTheCounterpartOfThePrimary() {
        val playlist = playlist(
            track(
                videoId = "fcnDmrtj6Sk",
                videoType = "MUSIC_VIDEO_TYPE_OMV",
                counterpart = track(
                    videoId = "lFQdcPTTzSg",
                    videoType = "MUSIC_VIDEO_TYPE_ATV"
                )
            )
        )

        assertTrue(playlist.hasRequestedCounterpart("lFQdcPTTzSg"))
    }

    private fun playlist(vararg tracks: YoutubeMusicWatchTrack) = YoutubeMusicWatchPlaylist(
        tracks = tracks.toList(),
        playlistId = "RDAMVMlFQdcPTTzSg",
        lyricsBrowseId = "",
        relatedBrowseId = "",
        continuation = "CONT"
    )

    private fun track(
        videoId: String,
        videoType: String,
        counterpart: YoutubeMusicWatchTrack? = null
    ) = YoutubeMusicWatchTrack(
        videoId = videoId,
        title = "Dai Dai",
        artists = listOf(YoutubeMusicWatchArtist("Shakira, Burna Boy", "UC_ARTIST")),
        albumTitle = "",
        albumBrowseId = "",
        durationMs = 201_000L,
        thumbnailUrl = "",
        videoType = videoType,
        explicit = false,
        counterpart = counterpart
    )
}
