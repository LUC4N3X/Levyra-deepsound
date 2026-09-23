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
import org.junit.Assert.assertFalse
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
        HighQualityAudioResolver(provider, HighQualityMappingStore(InMemoryMappingStorage()), scope)
    ).apply { this.mode = mode }

    private fun exactProvider(
        tier: AudioQualityTier = AudioQualityTier.KBPS_320,
        estimatedKbps: Int = tier.kbps
    ) = FakeHighQualityProvider(
        searchOutcome = { ProviderSearchOutcome.Found(listOf(candidate())) },
        lookupOutcome = { ProviderLookupOutcome.Found(candidate()) },
        streamOutcome = { ProviderStreamOutcome.Resolved(resolvedStream(it, tier, estimatedKbps = estimatedKbps)) }
    )

    private fun upgradeDecision(tier: AudioQualityTier, estimatedKbps: Int, normalKbps: Int): Track {
        val normal = normalTrack(averageBitrate = normalKbps * 1_000)
        return coordinator(exactProvider(tier, estimatedKbps)).play(normal = { normal })
    }

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
    fun upgradeDecisionUsesTheMeasuredBitrateAgainstTheNormalStream() {
        val cases = listOf(
            Triple(AudioQualityTier.KBPS_160, 248, 160) to true,
            Triple(AudioQualityTier.KBPS_160, 192, 160) to true,
            Triple(AudioQualityTier.KBPS_160, 175, 160) to false,
            Triple(AudioQualityTier.KBPS_160, 160, 128) to true,
            Triple(AudioQualityTier.KBPS_160, 160, 160) to false,
            Triple(AudioQualityTier.KBPS_96, 96, 160) to false,
            Triple(AudioQualityTier.KBPS_320, 321, 160) to true
        )
        cases.forEach { (case, upgraded) ->
            val (tier, estimated, normal) = case
            val result = upgradeDecision(tier, estimated, normal)
            assertEquals("$case", upgraded, result.playbackManifest?.alternativeSource != null)
        }
    }

    @Test
    fun streamMeasuredBetweenTiersIsReportedWithItsRealBitrate() {
        val result = upgradeDecision(AudioQualityTier.KBPS_160, 248, 160)
        val manifest = requireNotNull(result.playbackManifest)
        val descriptor = manifest.streams.single()
        assertEquals(248, manifest.alternativeSource?.bitrateKbps)
        assertEquals("~248 kbps", descriptor.qualityLabel)
        assertEquals(248_000, descriptor.bitrate)
        assertEquals(248_000, descriptor.averageBitrate)
        assertTrue(result.source.endsWith("~248 kbps"))
        assertFalse(result.source.contains("320"))
    }

    @Test
    fun verified320KeepsItsNominalLabelAndMeasuredAverage() {
        val result = upgradeDecision(AudioQualityTier.KBPS_320, 322, 160)
        val descriptor = requireNotNull(result.playbackManifest).streams.single()
        assertEquals(320, result.playbackManifest?.alternativeSource?.bitrateKbps)
        assertEquals("320 kbps", descriptor.qualityLabel)
        assertEquals(320_000, descriptor.bitrate)
        assertEquals(322_000, descriptor.averageBitrate)
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

    @Test
    fun cachedNormalStreamWaitsForAFirstHighQualityLookup() {
        val coordinator = coordinator(exactProvider())
        val requested = playbackTrack()
        assertTrue(coordinator.awaitsUpgrade(requested, normalTrack(), isVideoMode = false, audioQuality = "Auto"))
        coordinator.play(requested)
        assertTrue(!coordinator.awaitsUpgrade(requested, normalTrack(), isVideoMode = false, audioQuality = "Auto"))
        assertNotNull(coordinator.cachedUpgrade(requested, normalTrack(), isVideoMode = false, audioQuality = "Auto", provenance = provenance))
    }

    @Test
    fun videoModeAndDataSaverNeverWaitForAnUpgrade() {
        val coordinator = coordinator(exactProvider())
        val requested = playbackTrack()
        assertTrue(!coordinator.awaitsUpgrade(requested, normalTrack(), isVideoMode = true, audioQuality = "Auto"))
        assertTrue(!coordinator.awaitsUpgrade(requested, normalTrack(), isVideoMode = false, audioQuality = "Low"))
    }
}
