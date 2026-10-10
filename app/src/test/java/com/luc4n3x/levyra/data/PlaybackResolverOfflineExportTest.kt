package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackResolverOfflineExportTest {
    private val adaptiveMp4AudioWithoutToken =
        "https://rr3---sn-example.googlevideo.com/videoplayback?expire=1786904043&itag=140&" +
            "source=youtube&requiressl=yes&mime=audio%2Fmp4&gir=yes&clen=3168361&c=ANDROID_VR&lsig=abc"

    private val progressiveMuxed =
        "https://rr3---sn-example.googlevideo.com/videoplayback?expire=1786904043&itag=18&" +
            "source=youtube&requiressl=yes&mime=video%2Fmp4&ratebypass=yes&clen=15857332&c=ANDROID_VR&lsig=abc"

    @Test
    fun progressiveMuxedMp4IsAnOfflineExportSource() {
        assertTrue(isMuxedMp4ExportUrl(progressiveMuxed))
        assertFalse(isMuxedMp4ExportUrl(adaptiveMp4AudioWithoutToken))
        assertFalse(
            isMuxedMp4ExportUrl(
                "https://rr3---sn-example.googlevideo.com/videoplayback?itag=43&mime=video%2Fwebm&ratebypass=yes"
            )
        )
    }

    @Test
    fun offlineExportRejectsManifestsThatCannotServeTheWholeTrack() {
        assertTrue(supportsOfflineExport(manifestFor(progressiveMuxed)))
        assertFalse(supportsOfflineExport(manifestFor(adaptiveMp4AudioWithoutToken)))
        assertTrue(supportsOfflineExport(manifestFor("$adaptiveMp4AudioWithoutToken&pot=token-value")))
    }

    private fun manifestFor(audioUrl: String): ResolvedPlaybackManifest {
        return ResolvedPlaybackManifest(
            sourceVideoId = "abcdefghijk",
            provider = "LevyraExtractor",
            resolvedAtMs = 0L,
            expiresAtMs = 0L,
            durationMs = 180_000L,
            selectedAudioUrl = audioUrl,
            selectedVideoUrl = "",
            streams = listOf(
                PlaybackStreamDescriptor(
                    url = audioUrl,
                    kind = PlaybackStreamKind.AUDIO,
                    deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
                    container = if (isMuxedMp4ExportUrl(audioUrl)) "" else "m4a",
                    mimeType = if (isMuxedMp4ExportUrl(audioUrl)) "" else "audio/mp4",
                    selected = true
                )
            )
        )
    }


    @Test
    fun offlineCandidatesRequireAnMp4AudioContainer() {
        assertTrue(isMp4OfflineAudioCandidate("audio/mp4; codecs=mp4a.40.2", ""))
        assertTrue(isMp4OfflineAudioCandidate("MPEG_4", "https://example.com/audio"))
        assertTrue(isMp4OfflineAudioCandidate("", "https://example.com/audio.m4a?token=abc"))
        assertFalse(isMp4OfflineAudioCandidate("audio/mpeg", "https://example.com/audio.mp3"))
        assertFalse(isMp4OfflineAudioCandidate("audio/webm; codecs=opus", "https://example.com/audio.webm"))
        assertFalse(isMp4OfflineAudioCandidate("WEBMA_OPUS", "https://example.com/audio"))
    }
}
