package com.luc4n3x.levyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackResolverOfflineExportTest {
    @Test
    fun offlineCandidatesRequireAnMp4AudioContainer() {
        assertTrue(isMp4OfflineAudioCandidate("audio/mp4; codecs=mp4a.40.2", ""))
        assertTrue(isMp4OfflineAudioCandidate("MPEG_4", "https://example.com/audio"))
        assertTrue(isMp4OfflineAudioCandidate("", "https://example.com/audio.m4a?token=abc"))
        assertFalse(isMp4OfflineAudioCandidate("audio/mpeg", "https://example.com/audio.mp3"))
        assertFalse(isMp4OfflineAudioCandidate("audio/webm; codecs=opus", "https://example.com/audio.webm"))
        assertFalse(isMp4OfflineAudioCandidate("WEBMA_OPUS", "https://example.com/audio"))
    }

    @Test
    fun rejectedExtractorStreamsAreQuarantinedWithoutAffectingOtherProviders() {
        assertTrue(shouldQuarantineExtractor("LevyraExtractor · OPUS", "Response code: 403"))
        assertTrue(shouldQuarantineExtractor("LevyraExtractor HLS", "signature expired"))
        assertFalse(shouldQuarantineExtractor("YouTube Android", "Response code: 403"))
        assertFalse(shouldQuarantineExtractor("LevyraExtractor", "network timeout"))
    }
}
