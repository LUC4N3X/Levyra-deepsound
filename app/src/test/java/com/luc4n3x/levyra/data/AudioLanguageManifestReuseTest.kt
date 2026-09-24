package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlternativeAudioSource
import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.PlaybackStreamProvenance
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioLanguageManifestReuseTest {
    @Test
    fun `persisted language survives a restart with a different resolver generation`() {
        val persisted = PlaybackManifestCodec.encode(youtubeManifest(language = "it", generation = 1_700_000_000_000L))

        val restored = requireNotNull(PlaybackManifestCodec.decode(persisted))

        assertEquals("it", restored.provenance?.preferredAudioLanguage)
        assertEquals(1_700_000_000_000L, restored.provenance?.resolverGeneration)
        assertTrue(canReuse(restored, preferredLanguage = "it", languageRevision = 0L))
    }

    @Test
    fun `persisted default language is reusable after restart`() {
        val restored = roundTrip(youtubeManifest(language = "", generation = 42L))

        assertEquals("", restored.provenance?.preferredAudioLanguage)
        assertTrue(canReuse(restored, preferredLanguage = "", languageRevision = 0L))
        assertTrue(canReuse(restored, preferredLanguage = "", languageRevision = 3L))
    }

    @Test
    fun `changing the preference rejects the old language specific manifest`() {
        val restored = roundTrip(youtubeManifest(language = "it", generation = 42L))

        assertFalse(canReuse(restored, preferredLanguage = "es", languageRevision = 1L))
        assertFalse(canReuse(restored, preferredLanguage = "", languageRevision = 2L))
    }

    @Test
    fun `regional preference must match the persisted regional language exactly`() {
        val restored = roundTrip(youtubeManifest(language = "en-us", generation = 42L))

        assertTrue(canReuse(restored, preferredLanguage = "en-us", languageRevision = 0L))
        assertFalse(canReuse(restored, preferredLanguage = "en", languageRevision = 1L))
    }

    @Test
    fun `decoder normalizes persisted regional tags`() {
        val raw = JSONObject(PlaybackManifestCodec.encode(youtubeManifest(language = "", generation = 42L)))
        raw.getJSONObject("provenance").put("preferredAudioLanguage", "pt_BR")

        val restored = requireNotNull(PlaybackManifestCodec.decode(raw.toString()))

        assertEquals("pt-br", restored.provenance?.preferredAudioLanguage)
    }

    @Test
    fun `legacy manifest without persisted language keeps the stream language fallback`() {
        val raw = JSONObject(PlaybackManifestCodec.encode(youtubeManifest(language = "", generation = 42L)))
        raw.getJSONObject("provenance").remove("preferredAudioLanguage")
        val legacy = requireNotNull(PlaybackManifestCodec.decode(raw.toString()))

        assertNull(legacy.provenance?.preferredAudioLanguage)
        assertTrue(canReuse(legacy, preferredLanguage = "", languageRevision = 0L))
        assertFalse(canReuse(legacy, preferredLanguage = "", languageRevision = 1L))
        assertTrue(canReuse(legacy, preferredLanguage = "it", languageRevision = 1L))
        assertFalse(canReuse(legacy, preferredLanguage = "es", languageRevision = 1L))
    }

    @Test
    fun `jiosaavn alternative source is immune to youtube language preference changes`() {
        val alternative = roundTrip(
            youtubeManifest(language = "it", generation = 42L).copy(
                provider = "JioSaavn Audio",
                selectedAudioUrl = JIOSAAVN_URL,
                streams = emptyList(),
                alternativeSource = AlternativeAudioSource(
                    providerId = "jiosaavn",
                    providerTrackId = "js_12345",
                    bitrateKbps = 320,
                    verdict = AlternativeMatchVerdict.EXACT,
                    confidence = 100
                )
            )
        )

        assertEquals(320, alternative.alternativeSource?.bitrateKbps)
        assertEquals("jiosaavn", alternative.alternativeSource?.providerId)
        assertTrue(canReuse(alternative, preferredLanguage = "es", languageRevision = 5L, streamUrl = JIOSAAVN_URL))
        assertTrue(canReuse(alternative, preferredLanguage = "", languageRevision = 6L, streamUrl = JIOSAAVN_URL))
    }

    @Test
    fun `language blind fallback is rejected when a language is preferred`() {
        val hls = roundTrip(
            youtubeManifest(language = "it", generation = 42L).copy(
                streams = listOf(descriptor(url = AUDIO_URL, kind = PlaybackStreamKind.HLS))
            )
        )

        assertFalse(canReuse(hls, preferredLanguage = "it", languageRevision = 0L))
        val defaultHls = hls.copy(provenance = hls.provenance?.copy(preferredAudioLanguage = ""))
        assertTrue(canReuse(defaultHls, preferredLanguage = "", languageRevision = 0L))
    }

    private fun canReuse(
        manifest: ResolvedPlaybackManifest,
        preferredLanguage: String,
        languageRevision: Long,
        streamUrl: String = AUDIO_URL
    ): Boolean = AudioLanguageIntelligence.canReuseProvidedPlayback(
        manifest = manifest,
        streamUrl = streamUrl,
        preferredLanguage = preferredLanguage,
        languageRevision = languageRevision
    )

    private fun roundTrip(manifest: ResolvedPlaybackManifest): ResolvedPlaybackManifest =
        requireNotNull(PlaybackManifestCodec.decode(PlaybackManifestCodec.encode(manifest)))

    private fun youtubeManifest(language: String, generation: Long): ResolvedPlaybackManifest {
        val now = System.currentTimeMillis()
        return ResolvedPlaybackManifest(
            sourceVideoId = "dQw4w9WgXcQ",
            provider = "LevyraExtractor",
            resolvedAtMs = now,
            expiresAtMs = now + 3_600_000L,
            durationMs = 212_000L,
            selectedAudioUrl = AUDIO_URL,
            selectedVideoUrl = "",
            streams = listOf(descriptor(url = AUDIO_URL, kind = PlaybackStreamKind.AUDIO)),
            provenance = PlaybackStreamProvenance(
                clientName = "ANDROID_VR",
                resolverGeneration = generation,
                preferredAudioLanguage = language
            )
        )
    }

    private fun descriptor(url: String, kind: PlaybackStreamKind) = PlaybackStreamDescriptor(
        url = url,
        kind = kind,
        deliveryMethod = if (kind == PlaybackStreamKind.HLS) PlaybackDeliveryMethod.HLS else PlaybackDeliveryMethod.PROGRESSIVE,
        mimeType = "audio/webm",
        itag = 251,
        selected = true
    )

    private companion object {
        const val AUDIO_URL =
            "https://rr1.googlevideo.com/videoplayback?itag=251&expire=9999999999&clen=4500000&xtags=acont%3Doriginal%3Alang%3Dit"
        const val JIOSAAVN_URL = "https://aac.saavncdn.com/123/abc_320.mp4"
    }
}
