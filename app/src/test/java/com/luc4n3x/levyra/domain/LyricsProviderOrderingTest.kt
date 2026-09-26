package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsProviderOrderingTest {

    private val defaultOrdering = LyricsProviderOrdering()

    @Test
    fun defaultOrderingKeepsTheLegacyProviderChain() {
        assertEquals(
            listOf(
                LyricsProviderId.YOUTUBE_MUSIC,
                LyricsProviderId.LRCLIB_EXACT,
                LyricsProviderId.LRCLIB_SEARCH,
                LyricsProviderId.LYRICS_PLUS,
                LyricsProviderId.BINIMUM,
                LyricsProviderId.YOUTUBE_TRANSCRIPT,
                LyricsProviderId.LYRICS_OVH
            ),
            defaultOrdering.enabledIds
        )
    }

    @Test
    fun encodeDecodeRoundTripsOrderAndEnabledState() {
        val ordering = LyricsProviderOrdering(
            listOf(
                LyricsProviderEntry(LyricsProviderId.LRCLIB_EXACT),
                LyricsProviderEntry(LyricsProviderId.YOUTUBE_MUSIC),
                LyricsProviderEntry(LyricsProviderId.LYRICS_OVH, enabled = false),
                LyricsProviderEntry(LyricsProviderId.LYRICS_PLUS),
                LyricsProviderEntry(LyricsProviderId.LRCLIB_SEARCH),
                LyricsProviderEntry(LyricsProviderId.BINIMUM),
                LyricsProviderEntry(LyricsProviderId.YOUTUBE_TRANSCRIPT, enabled = false)
            )
        )
        assertEquals(ordering, LyricsProviderOrdering.decode(ordering.encode()))
    }

    @Test
    fun disabledProviderKeepsItsSlotAfterReordering() {
        val reordered = LyricsProviderOrdering(
            listOf(
                LyricsProviderEntry(LyricsProviderId.BINIMUM),
                LyricsProviderEntry(LyricsProviderId.YOUTUBE_MUSIC),
                LyricsProviderEntry(LyricsProviderId.LRCLIB_EXACT, enabled = false),
                LyricsProviderEntry(LyricsProviderId.LRCLIB_SEARCH),
                LyricsProviderEntry(LyricsProviderId.LYRICS_PLUS),
                LyricsProviderEntry(LyricsProviderId.YOUTUBE_TRANSCRIPT),
                LyricsProviderEntry(LyricsProviderId.LYRICS_OVH)
            )
        )
        val roundTrip = LyricsProviderOrdering.decode(reordered.encode())
        assertEquals(2, roundTrip.entries.indexOfFirst { it.id == LyricsProviderId.LRCLIB_EXACT })
        assertEquals(reordered.entries, roundTrip.entries)
        assertFalse(roundTrip.isEnabled(LyricsProviderId.LRCLIB_EXACT))
        assertEquals(
            listOf(
                LyricsProviderId.BINIMUM,
                LyricsProviderId.YOUTUBE_MUSIC,
                LyricsProviderId.LRCLIB_SEARCH,
                LyricsProviderId.LYRICS_PLUS,
                LyricsProviderId.YOUTUBE_TRANSCRIPT,
                LyricsProviderId.LYRICS_OVH
            ),
            roundTrip.enabledIds
        )
    }

    @Test
    fun unknownIdsAreDroppedAndMissingIdsAppendedInDefaultOrder() {
        val raw = "[{\"id\":\"lrclib_search\",\"enabled\":true},{\"id\":\"future_provider\",\"enabled\":true}]"
        val decoded = LyricsProviderOrdering.decode(raw)
        assertEquals(LyricsProviderId.LRCLIB_SEARCH, decoded.entries.first().id)
        assertEquals(LyricsProviderId.DEFAULT_ORDER.size, decoded.entries.size)
        assertTrue(decoded.entries.all { it.id in LyricsProviderId.DEFAULT_ORDER })
    }

    @Test
    fun onlyTheUntouchedOrderingIsDefault() {
        assertTrue(defaultOrdering.isDefault)
        val reordered = LyricsProviderOrdering(defaultOrdering.entries.reversed())
        assertFalse(reordered.isDefault)
        val disabled = LyricsProviderOrdering(defaultOrdering.entries.map { it.copy(enabled = it.id != LyricsProviderId.LYRICS_OVH) })
        assertFalse(disabled.isDefault)
    }

    @Test
    fun malformedPayloadDecodesToDefaultOrdering() {
        assertEquals(defaultOrdering, LyricsProviderOrdering.decode("not-json{{{["))
        assertEquals(defaultOrdering, LyricsProviderOrdering.decode(null))
        assertEquals(defaultOrdering, LyricsProviderOrdering.decode(""))
        assertEquals(defaultOrdering, LyricsProviderOrdering.decode("[]"))
    }

    @Test
    fun providerMatchingRecognizesRealAttributionStrings() {
        assertEquals(LyricsProviderId.YOUTUBE_MUSIC, LyricsProviderId.of("YouTube Music · Synced"))
        assertEquals(LyricsProviderId.LRCLIB_EXACT, LyricsProviderId.of("LRCLIB Exact"))
        assertEquals(LyricsProviderId.LRCLIB_SEARCH, LyricsProviderId.of("LRCLIB Search"))
        assertEquals(LyricsProviderId.LYRICS_PLUS, LyricsProviderId.of("LyricsPlus · lyricify.com"))
        assertEquals(LyricsProviderId.BINIMUM, LyricsProviderId.of("Binimum · Word"))
        assertEquals(LyricsProviderId.YOUTUBE_TRANSCRIPT, LyricsProviderId.of("YouTube Transcript Auto"))
        assertEquals(LyricsProviderId.LYRICS_OVH, LyricsProviderId.of("Lyrics.ovh"))
        assertNull(LyricsProviderId.of("Unknown Provider"))
    }

    @Test
    fun fetchPlanStartsPrimaryAloneAndGatesLastResortProviders() {
        val plan = LyricsFetchPlan.build(defaultOrdering)
        assertEquals(LyricsProviderId.YOUTUBE_MUSIC, plan.primary)
        assertEquals(
            listOf(
                LyricsProviderId.LRCLIB_EXACT,
                LyricsProviderId.LRCLIB_SEARCH,
                LyricsProviderId.LYRICS_PLUS,
                LyricsProviderId.BINIMUM
            ),
            plan.trusted
        )
        assertEquals(listOf(LyricsProviderId.YOUTUBE_TRANSCRIPT, LyricsProviderId.LYRICS_OVH), plan.lastResort)
    }

    @Test
    fun fetchPlanWithNoEnabledProvidersStartsNothing() {
        val ordering = LyricsProviderOrdering(
            LyricsProviderId.DEFAULT_ORDER.map { LyricsProviderEntry(it, enabled = false) }
        )
        val plan = LyricsFetchPlan.build(ordering)
        assertNull(plan.primary)
        assertTrue(plan.trusted.isEmpty())
        assertTrue(plan.lastResort.isEmpty())
    }

    @Test
    fun optimalResultRequiresSyncedAndSolidConfidence() {
        assertTrue(isOptimalLyricsResult(synced = true, confidence = 94))
        assertTrue(isOptimalLyricsResult(synced = true, confidence = 80))
        assertFalse(isOptimalLyricsResult(synced = true, confidence = 79))
        assertFalse(isOptimalLyricsResult(synced = false, confidence = 100))
    }
}

class ResumePlaybackPolicyTest {

    @Test
    fun resumesWhenQueueRestoredAndNotPlaying() {
        assertEquals(
            ResumePlaybackPolicy.Decision.RESUME,
            ResumePlaybackPolicy.decide(playing = false, hasRemotePlayback = false, currentTrackIsLiveRadio = false, restoredTrackAvailable = true)
        )
    }

    @Test
    fun alreadyPlayingIsANoOp() {
        assertEquals(
            ResumePlaybackPolicy.Decision.ALREADY_PLAYING,
            ResumePlaybackPolicy.decide(playing = true, hasRemotePlayback = false, currentTrackIsLiveRadio = false, restoredTrackAvailable = true)
        )
    }

    @Test
    fun emptyQueueFailsGracefully() {
        assertEquals(
            ResumePlaybackPolicy.Decision.NOTHING_TO_RESUME,
            ResumePlaybackPolicy.decide(playing = false, hasRemotePlayback = false, currentTrackIsLiveRadio = false, restoredTrackAvailable = false)
        )
    }

    @Test
    fun liveRadioIsNeverResumedAsAPersistedTrack() {
        assertEquals(
            ResumePlaybackPolicy.Decision.LIVE_RADIO,
            ResumePlaybackPolicy.decide(playing = false, hasRemotePlayback = false, currentTrackIsLiveRadio = true, restoredTrackAvailable = true)
        )
    }

    @Test
    fun remotePlaybackOwnershipIsNeverChallenged() {
        assertEquals(
            ResumePlaybackPolicy.Decision.REMOTE_PLAYBACK,
            ResumePlaybackPolicy.decide(playing = false, hasRemotePlayback = true, currentTrackIsLiveRadio = false, restoredTrackAvailable = true)
        )
    }
}
