package com.luc4n3x.levyra.player.offline

import com.luc4n3x.levyra.data.hqaudio.normalManifest
import com.luc4n3x.levyra.data.hqaudio.playbackTrack
import com.luc4n3x.levyra.domain.AlternativeAudioSource
import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifiedLosslessDownloadTest {
    private fun flac() = normalManifest().let { manifest ->
        val url = "https://api.arcod.xyz/v2/stream/play?t=synthetic"
        playbackTrack(url, manifest.copy(
            selectedAudioUrl = url,
            alternativeSource = AlternativeAudioSource("arcod", "7400", 900, AlternativeMatchVerdict.EXACT, 100),
            streams = listOf(manifest.streams.first().copy(url = url, codec = "flac", mimeType = "audio/flac", container = "flac"))
        ))
    }

    @Test
    fun verifiedFlacAcceptsAudioAndBinaryMimeWithoutChangingLegacyMp4Policy() {
        assertTrue(isAllowedOfflineExportSource(flac()))
        assertFalse(isAllowedOfflineExportSource(playbackTrack()))
        assertTrue(flac().hasVerifiedLosslessSource())
        assertTrue(flac().hasFreshVerifiedLosslessSource())
        val expired = flac().let { track -> track.copy(playbackManifest = track.playbackManifest!!.copy(
            streams = track.playbackManifest!!.streams.map { it.copy(expiresAtMs = 1L) }
        )) }
        assertFalse(expired.hasFreshVerifiedLosslessSource())
        assertTrue(allowsVerifiedFlacDownload(flac(), "audio/flac; charset=binary"))
        assertTrue(allowsVerifiedFlacDownload(flac(), "application/octet-stream"))
        assertFalse(allowsVerifiedFlacDownload(flac(), "text/html"))
        assertFalse(allowsVerifiedFlacDownload(flac(), "audio/mp4"))
        assertFalse(allowsVerifiedFlacDownload(playbackTrack(), "audio/flac"))
    }

    @Test
    fun mismatchedUrlAndLossyCodecCannotBePublishedAsFlac() {
        val track = flac()
        assertFalse(track.copy(streamUrl = "https://other.example/file").hasVerifiedLosslessSource())
        val manifest = track.playbackManifest!!
        assertFalse(track.copy(playbackManifest = manifest.copy(
            streams = manifest.streams.map { it.copy(codec = "mp4a") }
        )).hasVerifiedLosslessSource())
    }
}
