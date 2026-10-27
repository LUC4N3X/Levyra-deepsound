package com.luc4n3x.levyra.player.offline

import com.luc4n3x.levyra.data.hqaudio.normalManifest
import com.luc4n3x.levyra.data.hqaudio.playbackTrack
import com.luc4n3x.levyra.domain.AlternativeAudioSource
import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import com.luc4n3x.levyra.domain.DownloadQualityChoice
import com.luc4n3x.levyra.domain.DownloadQualitySource
import com.luc4n3x.levyra.domain.LevyraAudioQuality
import com.luc4n3x.levyra.domain.LevyraDownloadPreset
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadQualityOptionsTest {
    private fun flac(bitDepth: Int = 16, sampleRateHz: Int = 44_100, averageBitrate: Int = 900_000): Track =
        normalManifest().let { manifest ->
            val url = "https://api.arcod.xyz/v2/stream/play?t=synthetic"
            playbackTrack(url, manifest.copy(
                selectedAudioUrl = url,
                alternativeSource = AlternativeAudioSource("arcod", "7400", 900, AlternativeMatchVerdict.EXACT, 100),
                streams = listOf(manifest.streams.first().copy(
                    url = url, codec = "flac", mimeType = "audio/flac", container = "flac",
                    bitDepth = bitDepth, sampleRate = sampleRateHz, averageBitrate = averageBitrate, bitrate = averageBitrate
                ))
            ))
        }

    private fun jioSaavn(kbps: Int = 320, codec: String = "mp4a"): Track = normalManifest().let { manifest ->
        val url = "https://aac.saavncdn.com/123/abc_$kbps.mp4"
        playbackTrack(url, manifest.copy(
            selectedAudioUrl = url,
            alternativeSource = AlternativeAudioSource("jiosaavn", "js1", kbps, AlternativeMatchVerdict.EXACT, 100),
            streams = listOf(manifest.streams.first().copy(
                url = url, codec = codec, mimeType = "audio/mp4", container = "mp4",
                bitrate = kbps * 1_000, averageBitrate = kbps * 1_000
            ))
        ))
    }

    private fun reelAudio(averageBitrate: Int = 129_000): Track = normalManifest(averageBitrate).let { manifest ->
        val url = "https://rr1---sn-test.googlevideo.com/videoplayback?itag=140&mime=audio%2Fmp4&ratebypass=yes&clen=3200000"
        playbackTrack(url, manifest.copy(
            provider = "YouTube Android Reel Audio",
            selectedAudioUrl = url,
            streams = listOf(manifest.streams.first().copy(url = url))
        )).copy(source = "YouTube Android Reel Audio")
    }

    private fun reelMuxed(): Track = normalManifest().let { manifest ->
        val url = "https://rr1---sn-test.googlevideo.com/videoplayback?itag=18&mime=video%2Fmp4&ratebypass=yes&clen=9000000"
        playbackTrack(url, manifest.copy(
            provider = "YouTube Android Reel",
            selectedAudioUrl = url,
            streams = listOf(manifest.streams.first().copy(
                url = url, kind = PlaybackStreamKind.MUXED, mimeType = "video/mp4", container = "mp4",
                codec = "avc1.42001E, mp4a.40.2", bitrate = 600_000, averageBitrate = 550_000
            ))
        )).copy(source = "YouTube Android Reel · muxed")
    }

    private fun option(source: DownloadQualitySource, kbps: Int = 0, bitDepth: Int = 0, sampleRateHz: Int = 0, codec: String = "AAC") =
        DownloadQualityOption(source, if (source == DownloadQualitySource.Lossless) "FLAC" else codec, kbps, bitDepth, sampleRateHz)

    @Test
    fun ranksLosslessThenBitrateAndKeepsAtMostThree() {
        val ranked = rankDownloadQualityOptions(listOf(
            option(DownloadQualitySource.Standard, kbps = 129),
            option(DownloadQualitySource.HighBitrate, kbps = 320),
            option(DownloadQualitySource.Standard, kbps = 48),
            option(DownloadQualitySource.Lossless, bitDepth = 16, sampleRateHz = 44_100),
            option(DownloadQualitySource.Lossless, bitDepth = 24, sampleRateHz = 96_000)
        ))

        assertEquals(MAX_DOWNLOAD_QUALITY_OPTIONS, ranked.size)
        assertEquals(DownloadQualityTier.HiResLossless, ranked[0].tier)
        assertEquals(DownloadQualityTier.Lossless, ranked[1].tier)
        assertEquals(DownloadQualityTier.High, ranked[2].tier)
    }

    @Test
    fun keepsOneOrTwoRealOptionsWithoutPadding() {
        assertEquals(1, rankDownloadQualityOptions(listOf(option(DownloadQualitySource.Standard, kbps = 129))).size)
        val two = rankDownloadQualityOptions(listOf(
            option(DownloadQualitySource.Standard, kbps = 129),
            option(DownloadQualitySource.HighBitrate, kbps = 320)
        ))
        assertEquals(listOf(320, 129), two.map { it.kbps })
        assertTrue(rankDownloadQualityOptions(emptyList()).isEmpty())
    }

    @Test
    fun collapsesNearIdenticalLossyStreams() {
        val ranked = rankDownloadQualityOptions(listOf(
            option(DownloadQualitySource.HighBitrate, kbps = 160),
            option(DownloadQualitySource.Standard, kbps = 155),
            option(DownloadQualitySource.Standard, kbps = 0)
        ))
        assertEquals(listOf(160, 0), ranked.map { it.kbps })
        assertEquals(DownloadQualitySource.HighBitrate, ranked.first().source)
    }

    @Test
    fun verifiedFlacExposesRealBitDepthSampleRateAndSize() {
        val option = flac(bitDepth = 24, sampleRateHz = 96_000, averageBitrate = 2_800_000).downloadQualityOption(DownloadQualitySource.Lossless)!!

        assertEquals("FLAC", option.codecLabel)
        assertEquals(24, option.bitDepth)
        assertEquals(96_000, option.sampleRateHz)
        assertEquals(DownloadQualityTier.HiResLossless, option.tier)
        assertEquals(2_800_000L * 200_000L / 8_000L, option.estimatedBytes)
        assertEquals(DownloadQualityTier.Lossless, flac().downloadQualityOption(DownloadQualitySource.Lossless)!!.tier)
    }

    @Test
    fun neverLabelsUnverifiedSourcesAsLosslessOrHighBitrate() {
        assertNull(reelAudio().downloadQualityOption(DownloadQualitySource.Lossless))
        assertNull(reelAudio().downloadQualityOption(DownloadQualitySource.HighBitrate))
        assertNull(jioSaavn(codec = "flac").downloadQualityOption(DownloadQualitySource.HighBitrate))
        assertNull(playbackTrack().downloadQualityOption(DownloadQualitySource.Standard))
        assertNull(normalManifest().let { playbackTrack(it.selectedAudioUrl, it) }.downloadQualityOption(DownloadQualitySource.Standard))
        assertNull(flac().downloadQualityOption(DownloadQualitySource.Standard))
    }

    @Test
    fun jioSaavnBitrateComesFromTheValidatedStream() {
        val option = jioSaavn().downloadQualityOption(DownloadQualitySource.HighBitrate)!!
        assertEquals("AAC", option.codecLabel)
        assertEquals(320, option.kbps)
        assertEquals(DownloadQualityTier.High, option.tier)
        assertTrue(jioSaavn().hasVerifiedHighBitrateSource())
        assertTrue(isAllowedOfflineExportSource(jioSaavn()))
    }

    @Test
    fun youtubeAudioUsesManifestBitrateAndContentLengthButMuxedKeepsBitrateUnknown() {
        val audio = reelAudio().downloadQualityOption(DownloadQualitySource.Standard)!!
        assertEquals("AAC", audio.codecLabel)
        assertEquals(129, audio.kbps)
        assertEquals(3_200_000L, audio.estimatedBytes)

        val muxed = reelMuxed().downloadQualityOption(DownloadQualitySource.Standard)!!
        assertEquals("AAC", muxed.codecLabel)
        assertEquals(0, muxed.kbps)
        assertEquals(0L, muxed.estimatedBytes)
        assertEquals(DownloadQualityTier.Standard, muxed.tier)
    }

    @Test
    fun chosenQualityRejectsHiddenDowngrades() {
        val hiRes = DownloadQualityChoice(DownloadQualitySource.Lossless, bitDepth = 24, sampleRateHz = 96_000)
        assertTrue(hiRes.isSatisfiedBy(flac(24, 96_000)))
        assertFalse(hiRes.isSatisfiedBy(flac(16, 44_100)))
        assertFalse(hiRes.isSatisfiedBy(jioSaavn()))

        val high = DownloadQualityChoice(DownloadQualitySource.HighBitrate, kbps = 320)
        assertTrue(high.isSatisfiedBy(jioSaavn(320)))
        assertFalse(high.isSatisfiedBy(jioSaavn(160)))
        assertFalse(high.isSatisfiedBy(reelAudio()))

        val standard = DownloadQualityChoice(DownloadQualitySource.Standard, kbps = 129)
        assertTrue(standard.isSatisfiedBy(reelAudio(129_000)))
        assertFalse(standard.isSatisfiedBy(reelAudio(48_000)))
        assertFalse(standard.isSatisfiedBy(reelMuxed()))
        assertFalse(standard.isSatisfiedBy(flac()))
        assertTrue(DownloadQualityChoice(DownloadQualitySource.Standard).isSatisfiedBy(reelMuxed()))
    }

    @Test
    fun selectionPreselectsBestButKeepsAnExplicitUserChoice() {
        val lossless = option(DownloadQualitySource.Lossless, bitDepth = 16, sampleRateHz = 44_100)
        val standard = option(DownloadQualitySource.Standard, kbps = 129)

        assertEquals(standard.choice, reconcileDownloadQualitySelection(listOf(standard), null, false))
        assertEquals(lossless.choice, reconcileDownloadQualitySelection(listOf(lossless, standard), standard.choice, false))
        assertEquals(standard.choice, reconcileDownloadQualitySelection(listOf(lossless, standard), standard.choice, true))
        assertEquals(lossless.choice, reconcileDownloadQualitySelection(listOf(lossless), standard.choice, true))
        assertNull(reconcileDownloadQualitySelection(emptyList(), standard.choice, true))
    }

    @Test
    fun probeSnapshotDrivesLoadingReadyUnavailableAndErrorStates() {
        val accumulator = DownloadQualityProbeAccumulator(sources = 3)
        assertEquals(DownloadQualityPhase.Loading, accumulator.snapshot().phase)

        val afterFailure = accumulator.record(DownloadQualityProbeOutcome.Failed)
        assertEquals(DownloadQualityPhase.Loading, afterFailure.phase)

        val ready = accumulator.record(DownloadQualityProbeOutcome.Found(option(DownloadQualitySource.Standard, kbps = 129)))
        assertEquals(DownloadQualityPhase.Ready, ready.phase)
        assertTrue(ready.checkingMore)

        val done = accumulator.record(DownloadQualityProbeOutcome.Unavailable)
        assertEquals(DownloadQualityPhase.Ready, done.phase)
        assertFalse(done.checkingMore)

        assertEquals(DownloadQualityPhase.Unavailable, DownloadQualityProbeSnapshot().phase)
        assertEquals(DownloadQualityPhase.Error, DownloadQualityProbeSnapshot(failed = 1).phase)
    }

    @Test
    fun resolverQualityFollowsTheChosenSource() {
        val saver = LevyraDownloadSettings(preset = LevyraDownloadPreset.DataSaver)
        assertEquals(LevyraAudioQuality.HIGH, downloadResolverAudioQuality(DownloadQualitySource.Lossless, saver))
        assertEquals(LevyraAudioQuality.HIGH, downloadResolverAudioQuality(DownloadQualitySource.HighBitrate, saver))
        assertEquals("Low", downloadResolverAudioQuality(DownloadQualitySource.Standard, saver))
        assertNull(downloadResolverAudioQuality(null, LevyraDownloadSettings()))
    }
}
