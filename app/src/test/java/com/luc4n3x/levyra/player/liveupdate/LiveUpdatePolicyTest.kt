package com.luc4n3x.levyra.player.liveupdate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
