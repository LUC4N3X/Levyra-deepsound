package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoQualityLadderTest {

    private fun descriptor(
        kind: PlaybackStreamKind,
        url: String = "https://cdn.test/v/$kind",
        height: Int = 0,
        itag: Int = 0,
        bitrate: Int = 0,
        codec: String = "avc1.640028",
        mimeType: String = "video/mp4; codecs=\"avc1.640028\"",
        qualityLabel: String = "",
        width: Int = 0,
        fps: Int = 0,
        selected: Boolean = false
    ) = PlaybackStreamDescriptor(
        url = url,
        kind = kind,
        deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
        mimeType = mimeType,
        codec = codec,
        bitrate = bitrate,
        width = width,
        height = height,
        fps = fps,
        itag = itag,
        qualityLabel = qualityLabel,
        selected = selected
    )

    private val adaptiveLadder = listOf(
        descriptor(PlaybackStreamKind.VIDEO, "u144", 144, itag = 160, bitrate = 100_000, qualityLabel = "144p"),
        descriptor(PlaybackStreamKind.VIDEO, "u240", 240, itag = 133, bitrate = 250_000, qualityLabel = "240p"),
        descriptor(PlaybackStreamKind.VIDEO, "u360", 360, itag = 134, bitrate = 600_000, qualityLabel = "360p"),
        descriptor(PlaybackStreamKind.VIDEO, "u480", 480, itag = 135, bitrate = 1_000_000, qualityLabel = "480p"),
        descriptor(PlaybackStreamKind.VIDEO, "u720", 720, itag = 136, bitrate = 2_500_000, qualityLabel = "720p"),
        descriptor(PlaybackStreamKind.VIDEO, "u1080", 1080, itag = 137, bitrate = 4_000_000, qualityLabel = "1080p")
    )

    @Test
    fun buildsOneRungPerLabelSortedHighToLow() {
        val rungs = VideoQualityLadder.build(adaptiveLadder)
        assertEquals(listOf("1080p", "720p", "480p", "360p", "240p", "144p"), rungs.map { it.label })
    }

    @Test
    fun progressiveWinsItsLabelOverAdaptive() {
        val rungs = VideoQualityLadder.build(
            listOf(
                descriptor(PlaybackStreamKind.VIDEO, "a360", 360, itag = 134, bitrate = 700_000, qualityLabel = "360p"),
                descriptor(PlaybackStreamKind.MUXED, "p360", 360, itag = 18, bitrate = 500_000, qualityLabel = "360p")
            )
        )
        val rung360 = rungs.single { it.height == 360 }
        assertTrue(rung360.progressive)
        assertEquals("p360", rung360.url)
    }

    @Test
    fun avcBeatsVp9WithinTheSameLabel() {
        val rungs = VideoQualityLadder.build(
            listOf(
                descriptor(
                    PlaybackStreamKind.VIDEO, "v1080", 1080, itag = 248, bitrate = 4_000_000,
                    codec = "vp9", mimeType = "video/webm; codecs=\"vp9\"", qualityLabel = "1080p"
                ),
                descriptor(
                    PlaybackStreamKind.VIDEO, "a1080", 1080, itag = 137, bitrate = 3_000_000,
                    codec = "avc1.640028", mimeType = "video/mp4; codecs=\"avc1.640028\"", qualityLabel = "1080p"
                )
            )
        )
        val rung = rungs.single()
        assertEquals("a1080", rung.url)
    }

    @Test
    fun higherBitrateWinsWithinSameCodecLabel() {
        val rungs = VideoQualityLadder.build(
            listOf(
                descriptor(PlaybackStreamKind.VIDEO, "low", 1080, itag = 137, bitrate = 1_000_000, qualityLabel = "1080p"),
                descriptor(PlaybackStreamKind.VIDEO, "high", 1080, itag = 137, bitrate = 8_000_000, qualityLabel = "1080p")
            )
        )
        assertEquals("high", rungs.single().url)
    }

    @Test
    fun deviceCapabilityFilterDropsUndecodableRungs() {
        val rungs = VideoQualityLadder.build(adaptiveLadder) { it.height <= 480 }
        assertEquals(listOf("480p", "360p", "240p", "144p"), rungs.map { it.label })
    }

    @Test
    fun capabilityFilterNeverOffersAnUnknownCodecRung() {
        val rungs = VideoQualityLadder.build(
            listOf(
                descriptor(
                    PlaybackStreamKind.VIDEO, "x", 2160, itag = 313,
                    codec = "something-new", mimeType = "video/unknown", qualityLabel = "2160p"
                )
            ),
            decoderSupported = { false }
        )
        assertTrue(rungs.isEmpty())
    }

    @Test
    fun dropsLabelsWithoutResolvableHeight() {
        val rungs = VideoQualityLadder.build(
            listOf(descriptor(PlaybackStreamKind.VIDEO, "x", height = 0, qualityLabel = ""))
        )
        assertTrue(rungs.isEmpty())
    }

    @Test
    fun autoSelectsHighestRungAtOrBelowDeviceTarget() {
        val rungs = VideoQualityLadder.build(adaptiveLadder)
        val selected = VideoQualityLadder.selectRung(rungs, VideoQualityTarget.AUTO, autoTargetHeight = 720)
        assertEquals("720p", selected?.label)
    }

    @Test
    fun autoFallsBackToLowestRungWhenLadderIsAboveTarget() {
        val rungs = VideoQualityLadder.build(adaptiveLadder.filter { it.height >= 720 })
        val selected = VideoQualityLadder.selectRung(rungs, VideoQualityTarget.AUTO, autoTargetHeight = 480)
        assertEquals("720p", selected?.label)
    }

    @Test
    fun explicitTargetSelectsExactLabel() {
        val rungs = VideoQualityLadder.build(adaptiveLadder)
        val selected = VideoQualityLadder.selectRung(rungs, VideoQualityTarget.P1080, autoTargetHeight = 0)
        assertEquals("1080p", selected?.label)
    }

    @Test
    fun explicitTargetUnavailableUsesBestRungAtOrBelowTarget() {
        val rungs = VideoQualityLadder.build(adaptiveLadder.filter { it.height <= 1080 })
        val selected = VideoQualityLadder.selectRung(rungs, VideoQualityTarget.P1440, autoTargetHeight = 0)
        assertEquals("1080p", selected?.label)
    }

    @Test
    fun explicitTargetBelowLadderMinimumFailsSoftToLowestRung() {
        val rungs = VideoQualityLadder.build(adaptiveLadder.filter { it.height >= 480 })
        val selected = VideoQualityLadder.selectRung(rungs, VideoQualityTarget.P360, autoTargetHeight = 0)
        assertEquals("480p", selected?.label)
    }

    @Test
    fun emptyLadderYieldsNoSelection() {
        assertNull(VideoQualityLadder.selectRung(emptyList(), VideoQualityTarget.P1080, 0))
    }

    @Test
    fun rungBelowStepsExactlyOneRung() {
        val rungs = VideoQualityLadder.build(adaptiveLadder)
        assertEquals("480p", VideoQualityLadder.rungBelow(rungs, "720p")?.label)
        assertEquals("144p", VideoQualityLadder.rungBelow(rungs, "240p")?.label)
        assertNull(VideoQualityLadder.rungBelow(rungs, "144p"))
        assertNull(VideoQualityLadder.rungBelow(rungs, "unknown"))
        assertNull(VideoQualityLadder.rungBelow(rungs, null))
    }

    @Test
    fun activeLabelMatchesAdaptiveAndProgressiveUrls() {
        val rungs = VideoQualityLadder.build(
            adaptiveLadder + descriptor(PlaybackStreamKind.MUXED, "muxed360", 360, itag = 18, qualityLabel = "360p")
        )
        val adaptive = testTrack(streamUrl = "audio", videoStreamUrl = "u720")
        assertEquals("720p", VideoQualityLadder.activeLabelFor(adaptive, rungs))
        val progressive = testTrack(streamUrl = "muxed360", videoStreamUrl = "")
        assertEquals("360p", VideoQualityLadder.activeLabelFor(progressive, rungs))
        assertNull(VideoQualityLadder.activeLabelFor(testTrack(streamUrl = "sabr://x", videoStreamUrl = ""), rungs))
    }

    private fun testTrack(streamUrl: String, videoStreamUrl: String) = Track(
        id = "track-1",
        title = "Title",
        artist = "Artist",
        album = "",
        durationMs = 100_000L,
        streamUrl = streamUrl,
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        videoStreamUrl = videoStreamUrl
    )

    @Test
    fun autoTargetHeightFollowsDeviceProfile() {
        assertEquals(720, VideoQualityLadder.autoTargetHeight(lowRam = true, powerSave = false, unmetered = true, fastTransport = true, displayShortSidePx = 1200, hasHardwareAv1 = false))
        assertEquals(720, VideoQualityLadder.autoTargetHeight(lowRam = false, powerSave = false, unmetered = false, fastTransport = true, displayShortSidePx = 1200, hasHardwareAv1 = false))
        assertEquals(2160, VideoQualityLadder.autoTargetHeight(lowRam = false, powerSave = false, unmetered = true, fastTransport = true, displayShortSidePx = 1800, hasHardwareAv1 = true))
        assertEquals(1440, VideoQualityLadder.autoTargetHeight(lowRam = false, powerSave = false, unmetered = true, fastTransport = true, displayShortSidePx = 1440, hasHardwareAv1 = false))
        assertEquals(1080, VideoQualityLadder.autoTargetHeight(lowRam = false, powerSave = false, unmetered = true, fastTransport = true, displayShortSidePx = 1080, hasHardwareAv1 = false))
        assertEquals(720, VideoQualityLadder.autoTargetHeight(lowRam = false, powerSave = false, unmetered = true, fastTransport = true, displayShortSidePx = 800, hasHardwareAv1 = false))
    }

    @Test
    fun targetResolutionPrefersExplicitHeightOverAuto() {
        assertEquals(720, VideoQualityTarget.resolveHeight(VideoQualityTarget.AUTO, 720))
        assertEquals(2160, VideoQualityTarget.resolveHeight(VideoQualityTarget.P2160, 720))
    }

    @Test
    fun targetStorageRoundTripsAndUnknownFallsBackToAuto() {
        assertEquals(VideoQualityTarget.P1080, VideoQualityTarget.fromStorage("1080p"))
        assertEquals(VideoQualityTarget.AUTO, VideoQualityTarget.fromStorage(null))
        assertEquals(VideoQualityTarget.AUTO, VideoQualityTarget.fromStorage("bogus"))
    }

    @Test
    fun videoManifestCompactionKeepsOneCandidatePerHeightAndAudioFallback() {
        val selected = descriptor(
            PlaybackStreamKind.VIDEO,
            url = "selected-1080",
            height = 1080,
            bitrate = 4_000_000,
            selected = true
        )
        val manifest = ResolvedPlaybackManifest(
            sourceVideoId = "abc",
            provider = "test",
            resolvedAtMs = 0L,
            expiresAtMs = 0L,
            durationMs = 100_000L,
            selectedAudioUrl = "audio",
            selectedVideoUrl = selected.url,
            streams = listOf(
                selected,
                descriptor(PlaybackStreamKind.AUDIO, url = "audio", bitrate = 160_000),
                descriptor(PlaybackStreamKind.MUXED, url = "muxed-720", height = 720, bitrate = 2_000_000),
                descriptor(PlaybackStreamKind.VIDEO, url = "adaptive-720", height = 720, bitrate = 3_000_000),
                descriptor(PlaybackStreamKind.VIDEO, url = "adaptive-480", height = 480, bitrate = 1_000_000)
            )
        )

        val compact = manifest.compact(maxStreams = 4, preferVideoRungs = true)

        assertEquals(listOf("selected-1080", "muxed-720", "adaptive-480", "audio"), compact.streams.map { it.url })
    }

    @Test
    fun qualitySwitchUpdatesTrackUrlsAndManifestSelectionAtomically() {
        val audio = descriptor(PlaybackStreamKind.AUDIO, url = "audio", selected = true)
        val video720 = descriptor(PlaybackStreamKind.VIDEO, url = "video-720", height = 720, selected = true)
        val video1080 = descriptor(PlaybackStreamKind.VIDEO, url = "video-1080", height = 1080)
        val manifest = ResolvedPlaybackManifest(
            sourceVideoId = "abc",
            provider = "test",
            resolvedAtMs = 0L,
            expiresAtMs = 0L,
            durationMs = 100_000L,
            selectedAudioUrl = audio.url,
            selectedVideoUrl = video720.url,
            streams = listOf(audio, video720, video1080)
        )
        val switched = testTrack(audio.url, video720.url)
            .copy(playbackManifest = manifest)
            .withSelectedVideoQuality(
                VideoQualityRung("1080p", 1080, 1920, 137, 4_000_000, "video/mp4", "avc1", video1080.url, false),
                audio.url
            )

        assertEquals(audio.url, switched.streamUrl)
        assertEquals(video1080.url, switched.videoStreamUrl)
        assertEquals(audio.url, switched.playbackManifest?.selectedAudioUrl)
        assertEquals(video1080.url, switched.playbackManifest?.selectedVideoUrl)
        assertEquals(setOf(audio.url, video1080.url), switched.playbackManifest?.streams?.filter { it.selected }?.map { it.url }?.toSet())
    }
}

class VideoRebufferPolicyTest {

    @Test
    fun singleMidPlayStallDoesNotDowngrade() {
        val policy = VideoRebufferPolicy()
        policy.onMidPlayStall(10_000L)
        assertFalse(policy.shouldDowngrade(10_000L))
    }

    @Test
    fun twoStallsWithinWindowDowngrade() {
        val policy = VideoRebufferPolicy()
        policy.onMidPlayStall(10_000L)
        policy.onMidPlayStall(20_000L)
        assertTrue(policy.shouldDowngrade(20_000L))
    }

    @Test
    fun stallsOutsideTheWindowDoNotDowngrade() {
        val policy = VideoRebufferPolicy()
        policy.onMidPlayStall(10_000L)
        policy.onMidPlayStall(70_000L)
        assertFalse(policy.shouldDowngrade(70_000L))
    }

    @Test
    fun windowIsMeasuredFromTheStallTimestampsNotTheCheckTime() {
        val policy = VideoRebufferPolicy()
        policy.onMidPlayStall(0L)
        policy.onMidPlayStall(30_000L)
        assertTrue(policy.shouldDowngrade(30_000L))
        assertTrue(policy.shouldDowngrade(45_000L))
        assertFalse(policy.shouldDowngrade(45_001L))
    }

    @Test
    fun resetClearsTheStallHistory() {
        val policy = VideoRebufferPolicy()
        policy.onMidPlayStall(0L)
        policy.reset()
        policy.onMidPlayStall(30_000L)
        assertFalse(policy.shouldDowngrade(30_000L))
    }

    @Test
    fun classifierTreatsBufferingBeforeReadyAsPrepare() {
        assertEquals(VideoStallKind.PREPARE, classifyVideoStall(nowMs = 1_000L, reachedReady = false, lastSeekAtMs = null, lastQualitySwitchAtMs = null))
    }

    @Test
    fun classifierTreatsSeekWithinGraceAsSeek() {
        assertEquals(VideoStallKind.SEEK, classifyVideoStall(nowMs = 10_000L, reachedReady = true, lastSeekAtMs = 9_000L, lastQualitySwitchAtMs = null))
        assertEquals(VideoStallKind.MID_PLAY, classifyVideoStall(nowMs = 10_000L, reachedReady = true, lastSeekAtMs = 8_000L, lastQualitySwitchAtMs = null))
    }

    @Test
    fun classifierTreatsQualitySwitchWithinGraceAsSwitch() {
        assertEquals(VideoStallKind.QUALITY_SWITCH, classifyVideoStall(nowMs = 10_000L, reachedReady = true, lastSeekAtMs = null, lastQualitySwitchAtMs = 8_500L))
        assertEquals(VideoStallKind.MID_PLAY, classifyVideoStall(nowMs = 10_000L, reachedReady = true, lastSeekAtMs = null, lastQualitySwitchAtMs = 7_500L))
    }

    @Test
    fun classifierTreatsPlainMidPlayBufferingAsStall() {
        assertEquals(VideoStallKind.MID_PLAY, classifyVideoStall(nowMs = 10_000L, reachedReady = true, lastSeekAtMs = null, lastQualitySwitchAtMs = null))
    }
}

class AudioPartnerForAdaptiveRungTest {

    @Test
    fun prefersSelectedFreshAudioDescriptor() {
        val audio = PlaybackStreamDescriptor(
            url = "audio-251", kind = PlaybackStreamKind.AUDIO, deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
            averageBitrate = 160_000, itag = 251, selected = true
        )
        val manifest = ResolvedPlaybackManifest(
            sourceVideoId = "abc", provider = "test", resolvedAtMs = 0L, expiresAtMs = 0L,
            durationMs = 100_000L, selectedAudioUrl = "audio-251", selectedVideoUrl = "video-137",
            streams = listOf(audio)
        )
        assertEquals("audio-251", audioPartnerForAdaptiveRung(manifest, "fallback"))
    }

    @Test
    fun fallsBackToGivenUrlWhenManifestHasNoAudio() {
        val manifest = ResolvedPlaybackManifest(
            sourceVideoId = "abc", provider = "test", resolvedAtMs = 0L, expiresAtMs = 0L,
            durationMs = 100_000L, selectedAudioUrl = "", selectedVideoUrl = "",
            streams = emptyList()
        )
        assertEquals("fallback", audioPartnerForAdaptiveRung(manifest, "fallback"))
        assertEquals("fallback", audioPartnerForAdaptiveRung(null, "fallback"))
    }

    @Test
    fun muxedManifestNeverUsesTheMuxedUrlAsAudioPartner() {
        val muxed = PlaybackStreamDescriptor(
            url = "muxed-18", kind = PlaybackStreamKind.MUXED, deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
            itag = 18, selected = true
        )
        val manifest = ResolvedPlaybackManifest(
            sourceVideoId = "abc", provider = "test", resolvedAtMs = 0L, expiresAtMs = 0L,
            durationMs = 100_000L, selectedAudioUrl = "muxed-18", selectedVideoUrl = "",
            streams = listOf(muxed)
        )
        assertEquals("fallback", audioPartnerForAdaptiveRung(manifest, "fallback"))
    }

    @Test
    fun nullManifestReturnsFallback() {
        assertEquals("fallback", audioPartnerForAdaptiveRung(null, "fallback"))
    }
}
