package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.data.PlaybackBlockedException
import com.luc4n3x.levyra.domain.HighQualityAudioMode
import com.luc4n3x.levyra.domain.PlaybackStreamProvenance
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.LevyraPlaybackCacheKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class HighQualityPlaybackCoordinatorTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val provenance = { PlaybackStreamProvenance(networkRoute = "streaming-direct") }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun coordinator(
        provider: FakeHighQualityProvider,
        mode: HighQualityAudioMode = HighQualityAudioMode.AUTOMATIC
    ) = HighQualityPlaybackCoordinator(
        HighQualityAudioResolver(listOf(provider), HighQualityMappingStore(InMemoryMappingStorage()), scope)
    ).apply { this.mode = mode }

    private fun exactProvider(tier: AudioQualityTier = AudioQualityTier.KBPS_320) = FakeHighQualityProvider(
        searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate())) },
        streamOutcome = { ProviderStreamOutcome.Resolved(resolvedStream(it, tier)) }
    )

    private fun HighQualityPlaybackCoordinator.play(
        requested: Track = playbackTrack(),
        normal: suspend () -> Track = { normalTrack() }
    ): Track = runBlocking {
        val query = queryFor(requested, isVideoMode = false, audioQuality = "Auto") ?: error("not eligible")
        resolve(requested, query, provenance, normal)
    }

    @Test
    fun verifiedAlternativeKeepsTheOriginalTrackIdentity() {
        val requested = playbackTrack()
        val result = coordinator(exactProvider()).play(requested)
        assertEquals("https://aac.saavncdn.com/820/pW-kkdqr_320.mp4", result.streamUrl)
        assertEquals(requested.id, result.id)
        assertEquals(requested.title, result.title)
        assertEquals(requested.artist, result.artist)
        assertEquals(requested.album, result.album)
        assertEquals(requested.thumbnailUrl, result.thumbnailUrl)
        assertEquals(requested.largeThumbnailUrl, result.largeThumbnailUrl)
        assertEquals(requested.videoUrl, result.videoUrl)
        assertEquals(requested.durationMs, result.durationMs)
        val manifest = requireNotNull(result.playbackManifest)
        assertEquals(YOUTUBE_ID, manifest.sourceVideoId)
        assertEquals(320, manifest.alternativeSource?.bitrateKbps)
        assertEquals("pW-kkdqr", manifest.alternativeSource?.providerTrackId)
        assertTrue(manifest.isFresh())
        assertTrue(result.source.startsWith(HighQualityPlaybackCoordinator.SOURCE_LABEL))
        assertEquals("", result.videoStreamUrl)
        assertNull(result.youtubeLoudnessDb)
    }

    @Test
    fun alternativeUsesItsOwnMediaCacheNamespace() {
        val result = coordinator(exactProvider()).play()
        val alternativeKey = LevyraPlaybackCacheKey.stream(result)
        assertTrue(alternativeKey.contains(":stream-v2:alt-"))
        assertNotEquals(LevyraPlaybackCacheKey.stream(normalTrack()), alternativeKey)
    }

    @Test
    fun providerFailureKeepsNormalSource() {
        val provider = FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Failed(ProviderFailure.NETWORK) })
        val normal = normalTrack()
        val result = coordinator(provider).play(normal = { normal })
        assertSame(normal, result)
    }

    @Test
    fun alternativeNotMeaningfullyHigherKeepsNormalSource() {
        val normal = normalTrack(averageBitrate = 150_000)
        val result = coordinator(exactProvider(AudioQualityTier.KBPS_160)).play(normal = { normal })
        assertSame(normal, result)
    }

    @Test
    fun genuine160IsUsedWhenNormalStreamIsLower() {
        val result = coordinator(exactProvider(AudioQualityTier.KBPS_160)).play(normal = { normalTrack(averageBitrate = 128_000) })
        assertEquals(160, result.playbackManifest?.alternativeSource?.bitrateKbps)
    }

    @Test
    fun alternativeCoversNormalResolutionFailure() {
        val result = coordinator(exactProvider()).play(normal = { throw PlaybackBlockedException("Stream non disponibile") })
        assertEquals(320, result.playbackManifest?.alternativeSource?.bitrateKbps)
    }

    @Test
    fun normalFailureIsRethrownWhenNoAlternativeExists() {
        val provider = FakeHighQualityProvider()
        try {
            coordinator(provider).play(normal = { throw PlaybackBlockedException("Stream non disponibile") })
            fail("expected normal failure")
        } catch (error: PlaybackBlockedException) {
            assertEquals("Stream non disponibile", error.message)
        }
    }

    @Test
    fun ineligibleRequestsNeverQueryProvider() {
        val coordinator = coordinator(exactProvider())
        val track = playbackTrack()
        assertNull(coordinator.queryFor(track, isVideoMode = true, audioQuality = "Auto"))
        assertNull(coordinator.queryFor(track, isVideoMode = false, audioQuality = "Low"))
        assertNull(coordinator.queryFor(track.copy(source = "Offline"), isVideoMode = false, audioQuality = "Auto"))
        assertNull(coordinator.queryFor(track.copy(streamUrl = "content://media/1"), isVideoMode = false, audioQuality = "Auto"))
        assertNull(coordinator.queryFor(track.copy(durationMs = 0L), isVideoMode = false, audioQuality = "Auto"))
        coordinator.mode = HighQualityAudioMode.OFF
        assertNull(coordinator.queryFor(track, isVideoMode = false, audioQuality = "High"))
    }

    @Test
    fun explicitFlagIsOnlyTrustedWhenSet() {
        val coordinator = coordinator(exactProvider())
        assertNull(coordinator.queryFor(playbackTrack(), false, "Auto")?.explicit)
        assertEquals(true, coordinator.queryFor(playbackTrack().copy(explicit = true), false, "Auto")?.explicit)
    }

    @Test
    fun cachedUpgradeServesVerifiedStreamUntilDisabled() {
        val coordinator = coordinator(exactProvider())
        coordinator.play()
        val cached = coordinator.cachedUpgrade(playbackTrack(), normalTrack(), false, "Auto", provenance)
        assertEquals(320, cached?.playbackManifest?.alternativeSource?.bitrateKbps)
        coordinator.mode = HighQualityAudioMode.OFF
        assertNull(coordinator.cachedUpgrade(playbackTrack(), normalTrack(), false, "Auto", provenance))
    }

    @Test
    fun playerFailureReturnsToNormalSource() {
        val coordinator = coordinator(exactProvider())
        val alternative = coordinator.play()
        assertTrue(coordinator.handlesFailure(alternative))
        coordinator.reportFailure(alternative, "HTTP 403")
        assertNull(coordinator.cachedUpgrade(playbackTrack(), normalTrack(), false, "Auto", provenance))
        val normal = normalTrack()
        assertSame(normal, coordinator.play(normal = { normal }))
    }

    @Test
    fun alreadyUpgradedTrackDoesNotStartAnotherProviderLookup() {
        val provider = exactProvider()
        val coordinator = coordinator(provider)
        val alternative = coordinator.play()
        val searchesBefore = provider.searches.size
        assertNull(coordinator.queryFor(alternative, isVideoMode = false, audioQuality = "Auto"))
        assertEquals(searchesBefore, provider.searches.size)
    }

    private fun qobuzCoordinator(
        mode: HighQualityAudioMode = HighQualityAudioMode.AUTOMATIC,
        stream: (AlternativeTrackCandidate) -> ResolvedHighQualityStream = { losslessStream(it) },
        search: suspend (String) -> ProviderSearchOutcome = {
            ProviderSearchOutcome.Found(listOf(candidate(id = "9001", providerId = "qobuz")))
        }
    ) = HighQualityPlaybackCoordinator(
        HighQualityAudioResolver(
            listOf(
                FakeHighQualityProvider(searchOutcome = { ProviderSearchOutcome.Found(emptyList()) }),
                FakeHighQualityProvider(
                    id = "qobuz",
                    displayName = "Qobuz",
                    losslessCapable = true,
                    supportsLookup = false,
                    searchOutcome = search,
                    streamOutcome = { ProviderStreamOutcome.Resolved(stream(it)) }
                )
            ),
            HighQualityMappingStore(InMemoryMappingStorage()),
            scope,
            hedgeDelayMs = 0L
        )
    ).apply { this.mode = mode }

    @Test
    fun losslessQobuzStreamIsDescribedTruthfully() {
        val result = qobuzCoordinator(mode = HighQualityAudioMode.PREFER_320).play(normal = { normalTrack(averageBitrate = 256_000) })
        val manifest = result.playbackManifest!!
        val descriptor = manifest.streams.single()
        assertEquals("Levyra HQ · Qobuz · FLAC 24-bit 96 kHz", result.source)
        assertEquals("audio/flac", descriptor.mimeType)
        assertEquals(2_400_000, descriptor.bitrate)
        assertEquals(96_000, descriptor.sampleRate)
        assertEquals(24, descriptor.bitDepth)
        assertEquals("FLAC · 24-bit · 96 kHz", descriptor.qualityLabel)
        val source = manifest.alternativeSource!!
        assertEquals("qobuz", source.providerId)
        assertTrue(source.lossless)
        assertEquals(24, source.bitDepth)
        assertEquals(96_000, source.sampleRateHz)
        assertNotEquals(320, source.bitrateKbps)
    }

    @Test
    fun differentLosslessFormatsUseDifferentMediaCacheKeys() {
        val hiRes = qobuzCoordinator().play()
        val cd = qobuzCoordinator(stream = { losslessStream(it, bitDepth = 16, sampleRateHz = 44_100) }).play()
        assertNotEquals(LevyraPlaybackCacheKey.stream(hiRes), LevyraPlaybackCacheKey.stream(cd))
    }

    @Test
    fun jioSaavnSourceLabelKeepsItsBitrate() {
        val result = coordinator(exactProvider()).play()
        assertEquals("Levyra HQ · JioSaavn · 320 kbps", result.source)
        assertEquals("AAC 320 kbps", result.playbackManifest!!.streams.single().qualityLabel)
    }

    @Test
    fun normalSourceWinsWhenHighQualityCannotResolveInTheAutomaticWindow() {
        val coordinator = qobuzCoordinator(search = {
            kotlinx.coroutines.delay(8_000L)
            ProviderSearchOutcome.Found(emptyList())
        })
        val started = System.nanoTime()
        val result = coordinator.play()
        val elapsedMs = (System.nanoTime() - started) / 1_000_000L
        assertEquals(YOUTUBE_AUDIO_URL, result.streamUrl)
        assertNull(result.playbackManifest?.alternativeSource)
        assertTrue("elapsed=$elapsedMs", elapsedMs < HighQualityPlaybackCoordinator.AUTOMATIC_WAIT_MS + 1_000L)
    }
}
