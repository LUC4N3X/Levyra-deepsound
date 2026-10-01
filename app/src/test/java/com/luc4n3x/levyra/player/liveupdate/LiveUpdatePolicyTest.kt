package com.luc4n3x.levyra.player.liveupdate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveUpdatePolicyTest {
    @Test
    fun `live updates are gated to android 16 and newer`() {
        assertEquals(36, LiveUpdatePolicy.MIN_SDK)
        assertFalse(LiveUpdatePolicy.isSupported(26))
        assertFalse(LiveUpdatePolicy.isSupported(35))
        assertTrue(LiveUpdatePolicy.isSupported(36))
        assertTrue(LiveUpdatePolicy.isSupported(37))
    }

    @Test
    fun `clock formats minutes and hours`() {
        assertEquals("0:00", LiveUpdatePolicy.formatClock(-5L))
        assertEquals("2:31", LiveUpdatePolicy.formatClock(151_400L))
        assertEquals("4:17", LiveUpdatePolicy.formatClock(257_000L))
        assertEquals("1:02:03", LiveUpdatePolicy.formatClock(3_723_000L))
    }

    @Test
    fun `active playback maps real track data and a chronometer`() {
        val content = PlaybackLiveUpdateMapper.map(playing())!!

        assertEquals("track-1", content.mediaId)
        assertEquals("Enjoy the Silence", content.title)
        assertEquals("Depeche Mode · 4:17", content.text)
        assertEquals(NOW - 151_000L, content.chronometerBaseEpochMs)
        assertEquals(257_000L - 151_000L + PlaybackLiveUpdateMapper.TIMEOUT_GRACE_MS, content.timeoutMs)
    }

    @Test
    fun `paused stopped ended or empty playback hides the live update`() {
        assertNull(PlaybackLiveUpdateMapper.map(playing().copy(playWhenReady = false, playing = false)))
        assertNull(PlaybackLiveUpdateMapper.map(playing().copy(ready = false, buffering = false, playing = false)))
        assertNull(PlaybackLiveUpdateMapper.map(playing().copy(mediaId = null)))
        assertNull(PlaybackLiveUpdateMapper.map(playing().copy(title = "  ")))
    }

    @Test
    fun `buffering keeps the update without a running chronometer`() {
        val content = PlaybackLiveUpdateMapper.map(playing().copy(ready = false, buffering = true, playing = false))

        assertNotNull(content)
        assertNull(content!!.chronometerBaseEpochMs)
    }

    @Test
    fun `changed speed ads and dynamic streams do not use a misleading chronometer`() {
        assertNull(PlaybackLiveUpdateMapper.map(playing().copy(speed = 1.25f))!!.chronometerBaseEpochMs)
        assertNull(PlaybackLiveUpdateMapper.map(playing().copy(playingAd = true))!!.chronometerBaseEpochMs)

        val live = PlaybackLiveUpdateMapper.map(playing().copy(dynamic = true))!!
        assertNull(live.chronometerBaseEpochMs)
        assertEquals("Depeche Mode", live.text)
        assertEquals(PlaybackLiveUpdateMapper.UNKNOWN_DURATION_TIMEOUT_MS, live.timeoutMs)
    }

    @Test
    fun `known duration timeout follows wall clock time at the current speed`() {
        val remaining = 257_000L - 151_000L
        val grace = PlaybackLiveUpdateMapper.TIMEOUT_GRACE_MS

        assertEquals(remaining * 2 + grace, PlaybackLiveUpdateMapper.map(playing().copy(speed = 0.5f))!!.timeoutMs)
        assertEquals(remaining / 2 + grace, PlaybackLiveUpdateMapper.map(playing().copy(speed = 2f))!!.timeoutMs)
        assertEquals(remaining + grace, PlaybackLiveUpdateMapper.map(playing().copy(speed = 0f))!!.timeoutMs)
        assertEquals(remaining + grace, PlaybackLiveUpdateMapper.map(playing().copy(speed = Float.NaN))!!.timeoutMs)
    }

    @Test
    fun `speed change without a chronometer still refreshes the timeout`() {
        val half = PlaybackLiveUpdateMapper.map(playing().copy(speed = 0.5f))!!
        val quarter = PlaybackLiveUpdateMapper.map(playing().copy(speed = 0.25f))!!
        val sameHalf = PlaybackLiveUpdateMapper.map(playing().copy(speed = 0.5f))!!

        assertFalse(quarter.isEquivalentTo(half))
        assertTrue(sameHalf.isEquivalentTo(half))
    }

    @Test
    fun `unknown duration and missing artist degrade cleanly`() {
        assertEquals("Depeche Mode", PlaybackLiveUpdateMapper.map(playing().copy(durationMs = 0L))!!.text)
        assertEquals("4:17", PlaybackLiveUpdateMapper.map(playing().copy(artist = ""))!!.text)
    }

    @Test
    fun `equivalent content tolerates chronometer drift but not real changes`() {
        val first = PlaybackLiveUpdateMapper.map(playing())!!
        val drift = PlaybackLiveUpdateMapper.map(playing().copy(nowEpochMs = NOW + 300L, positionMs = 151_250L))!!
        val seek = PlaybackLiveUpdateMapper.map(playing().copy(positionMs = 30_000L))!!
        val paused = PlaybackLiveUpdateMapper.map(playing().copy(playing = false, ready = false, buffering = true))!!

        assertTrue(drift.isEquivalentTo(first))
        assertFalse(seek.isEquivalentTo(first))
        assertFalse(paused.isEquivalentTo(first))
        assertFalse(first.isEquivalentTo(null))
    }

    @Test
    fun `download live update shows percent and batch counter`() {
        val content = DownloadLiveUpdateMapper.map(
            progress = 64,
            batch = DownloadBatchProgress(title = "Road Trip", completed = 7, total = 20),
            promotionAllowed = true
        )

        assertEquals("64%", content.shortCriticalText)
        assertEquals("Road Trip · 7 / 20", content.subText)
        assertTrue(content.requestPromotion)
    }

    @Test
    fun `download live update stays plain for single items and finished or denied promotion`() {
        assertNull(DownloadLiveUpdateMapper.map(10, null, true).subText)
        assertNull(DownloadLiveUpdateMapper.map(10, DownloadBatchProgress("Single", 0, 1), true).subText)
        assertEquals("3 / 4", DownloadLiveUpdateMapper.map(10, DownloadBatchProgress(" ", 3, 4), true).subText)
        assertEquals("100%", DownloadLiveUpdateMapper.map(150, null, true).shortCriticalText)
        assertFalse(DownloadLiveUpdateMapper.map(100, null, true).requestPromotion)
        assertFalse(DownloadLiveUpdateMapper.map(50, null, false).requestPromotion)
    }

    @Test
    fun `only one download owns the promoted slot until it releases`() {
        assertTrue(DownloadLiveUpdateSlot.claim("a"))
        assertTrue(DownloadLiveUpdateSlot.claim("a"))
        assertFalse(DownloadLiveUpdateSlot.claim("b"))
        DownloadLiveUpdateSlot.release("b")
        assertFalse(DownloadLiveUpdateSlot.claim("b"))
        DownloadLiveUpdateSlot.release("a")
        assertTrue(DownloadLiveUpdateSlot.claim("b"))
        DownloadLiveUpdateSlot.release("b")
    }

    private fun playing() = PlaybackLiveUpdateInput(
        mediaId = "track-1",
        title = "Enjoy the Silence",
        artist = "Depeche Mode",
        playWhenReady = true,
        buffering = false,
        ready = true,
        playing = true,
        playingAd = false,
        dynamic = false,
        speed = 1f,
        positionMs = 151_000L,
        durationMs = 257_000L,
        nowEpochMs = NOW
    )

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
