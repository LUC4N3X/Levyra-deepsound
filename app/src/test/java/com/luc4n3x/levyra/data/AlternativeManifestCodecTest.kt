package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlternativeAudioSource
import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternativeManifestCodecTest {
    private val url = "https://aac.saavncdn.com/820/5ddb9a79a5218f85ca9bef170f3a461d_320.mp4"

    private fun manifest(source: AlternativeAudioSource?): ResolvedPlaybackManifest {
        val now = System.currentTimeMillis()
        return ResolvedPlaybackManifest(
            sourceVideoId = "4NRXx6U8ABQ",
            provider = "Levyra HQ · JioSaavn",
            resolvedAtMs = now,
            expiresAtMs = now + 3_600_000L,
            durationMs = 200_000L,
            selectedAudioUrl = url,
            selectedVideoUrl = "",
            streams = listOf(
                PlaybackStreamDescriptor(
                    url = url,
                    kind = PlaybackStreamKind.AUDIO,
                    deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
                    container = "mp4",
                    mimeType = "audio/mp4",
                    codec = "mp4a",
                    bitrate = 320_000,
                    averageBitrate = 316_000,
                    qualityLabel = "320 kbps",
                    expiresAtMs = now + 3_600_000L,
                    selected = true
                )
            ),
            alternativeSource = source
        )
    }

    @Test
    fun alternativeProvenanceSurvivesRoundTrip() {
        val source = AlternativeAudioSource("jiosaavn", "pW-kkdqr", 320, AlternativeMatchVerdict.EXACT, 100)
        val decoded = PlaybackManifestCodec.decode(PlaybackManifestCodec.encode(manifest(source)))!!
        assertEquals(source, decoded.alternativeSource)
        assertTrue(decoded.isAlternativeSource)
        assertEquals("4NRXx6U8ABQ", decoded.sourceVideoId)
    }

    @Test
    fun ordinaryManifestHasNoAlternativeSource() {
        val decoded = PlaybackManifestCodec.decode(PlaybackManifestCodec.encode(manifest(null)))!!
        assertNull(decoded.alternativeSource)
        assertFalse(decoded.isAlternativeSource)
    }

    @Test
    fun rejectedVerdictIsNeverRestoredAsAlternative() {
        val source = AlternativeAudioSource("jiosaavn", "pW-kkdqr", 320, AlternativeMatchVerdict.REJECTED, 0)
        assertNull(PlaybackManifestCodec.decode(PlaybackManifestCodec.encode(manifest(source)))!!.alternativeSource)
    }
}
