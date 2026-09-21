package com.luc4n3x.levyra.data.locallibrary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalReplayGainTagsTest {
    @Test
    fun readsStandardReplayGainTags() {
        val tags = parseLocalReplayGainTags(
            """
            REPLAYGAIN_TRACK_GAIN=-7.10 dB
            REPLAYGAIN_ALBUM_GAIN=-5,25 dB
            REPLAYGAIN_TRACK_PEAK=0.9231
            REPLAYGAIN_ALBUM_PEAK=0.9812
            """.trimIndent()
        )

        assertEquals(-7.10f, tags.trackGainDb ?: 0f, 0.001f)
        assertEquals(-5.25f, tags.albumGainDb ?: 0f, 0.001f)
        assertEquals(0.9231f, tags.trackPeak ?: 0f, 0.0001f)
        assertEquals(0.9812f, tags.albumPeak ?: 0f, 0.0001f)
    }

    @Test
    fun localTrackKeepsAlbumArtistAndReplayGainMetadata() {
        val row = ScannedLocalAudio(
            volumeName = "external_primary",
            mediaStoreId = 42L,
            contentUri = "content://media/external/audio/42",
            filePath = "/storage/emulated/0/Music/Compilation/track.flac",
            relativePath = "Music/Compilation/",
            displayName = "track.flac",
            title = "Track",
            artist = "Guest Artist",
            album = "Compilation",
            albumArtist = "Various Artists",
            genre = "Pop",
            year = 2026,
            trackField = 1,
            trackText = "1",
            discText = "1",
            durationMs = 180_000L,
            mimeType = "audio/flac",
            bitrate = 900_000,
            sizeBytes = 20_000_000L,
            dateAddedMs = 1L,
            dateModifiedMs = 1L,
            albumId = 7L
        ).toLocalMediaEntity("", 1L).copy(
            customTags = "REPLAYGAIN_TRACK_GAIN=-6.0 dB\nREPLAYGAIN_ALBUM_GAIN=-4.0 dB"
        )

        val track = row.toLocalTrack()

        assertEquals("Various Artists", track.albumArtist)
        assertEquals(-6f, track.replayGainTrackDb ?: 0f, 0.001f)
        assertEquals(-4f, track.replayGainAlbumDb ?: 0f, 0.001f)
    }

    @Test
    fun ignoresMalformedReplayGainValues() {
        val tags = parseLocalReplayGainTags(
            """
            REPLAYGAIN_TRACK_GAIN=not-a-number
            REPLAYGAIN_TRACK_PEAK=-1
            """.trimIndent()
        )

        assertNull(tags.trackGainDb)
        assertNull(tags.trackPeak)
    }
}
