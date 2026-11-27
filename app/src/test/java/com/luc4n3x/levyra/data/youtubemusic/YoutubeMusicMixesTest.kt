package com.luc4n3x.levyra.data.youtubemusic

import com.luc4n3x.levyra.data.YoutubeMusicPlaylistDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicMixesTest {
    private fun detail(id: String) = YoutubeMusicPlaylistDetail(id, id, "", "", "", emptyList())

    @Test
    fun onlyGeneratedMixesAreRecognized() {
        assertTrue(isYoutubeMusicGeneratedMix("RDTMAK5uy_lr0LWzGrq6FU9GIxWvFHTRPQD2LHMqlFA"))
        assertTrue(isYoutubeMusicGeneratedMix("VLRDTMAK5uy_kset8DisdE7LSD4TNjEVvrKRTmG7a56E"))
        assertFalse(isYoutubeMusicGeneratedMix("RDCLAK5uy_kmPRjHDECIcuVwnKsx2Ng7fyNgFKWNJFs"))
        assertFalse(isYoutubeMusicGeneratedMix("PL4fGSI1pDJn6O1LS0XSdF3RyO0Rq_LDeI"))
    }

    @Test
    fun snapshotStaysTheSameUntilItExpires() {
        var now = 0L
        val snapshots = YoutubeMusicMixSnapshots(clock = { now }, ttlMs = 1_000L, maxEntries = 4)
        val first = detail("a")
        snapshots.put("a", first)
        now = 999L
        assertSame(first, snapshots.get("a"))
        now = 1_000L
        assertNull(snapshots.get("a"))
    }

    @Test
    fun oldestSnapshotIsDroppedPastTheLimit() {
        val snapshots = YoutubeMusicMixSnapshots(clock = { 0L }, ttlMs = 1_000L, maxEntries = 2)
        snapshots.put("a", detail("a"))
        snapshots.put("b", detail("b"))
        snapshots.put("a", detail("a2"))
        snapshots.put("c", detail("c"))
        assertNull(snapshots.get("b"))
        assertEquals("a2", snapshots.get("a")?.playlistId)
        assertEquals("c", snapshots.get("c")?.playlistId)
    }
}
