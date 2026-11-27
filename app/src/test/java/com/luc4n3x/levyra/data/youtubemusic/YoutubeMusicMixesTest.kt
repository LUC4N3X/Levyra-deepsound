package com.luc4n3x.levyra.data.youtubemusic

import com.luc4n3x.levyra.data.YoutubeMusicPlaylistDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

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
    fun concurrentCallersShareOneLoad() = runBlocking {
        val snapshots = YoutubeMusicMixSnapshots(maxEntries = 4)
        val gate = CompletableDeferred<Unit>()
        val loads = AtomicInteger()
        val first = async(Dispatchers.Default) {
            snapshots.getOrLoad("a") { loads.incrementAndGet(); gate.await(); detail("first") }
        }
        while (loads.get() == 0) yield()
        val second = async(Dispatchers.Default) { snapshots.getOrLoad("a") { loads.incrementAndGet(); detail("other") } }
        gate.complete(Unit)
        assertSame(first.await(), second.await())
        assertEquals(1, loads.get())
    }

    @Test
    fun snapshotStaysUntilRefresh() = runBlocking {
        val snapshots = YoutubeMusicMixSnapshots(maxEntries = 4)
        val first = snapshots.getOrLoad("a") { detail("a1") }
        assertSame(first, snapshots.getOrLoad("a") { detail("a2") })
        snapshots.clear()
        assertNull(snapshots.get("a"))
        assertEquals("a3", snapshots.getOrLoad("a") { detail("a3") }?.playlistId)
    }

    @Test
    fun emptyLoadIsNotKept() = runBlocking {
        val snapshots = YoutubeMusicMixSnapshots(maxEntries = 4)
        assertNull(snapshots.getOrLoad("a") { null })
        assertEquals("a", snapshots.getOrLoad("a") { detail("a") }?.playlistId)
    }

    @Test
    fun oldestSnapshotIsDroppedPastTheLimit() = runBlocking {
        val snapshots = YoutubeMusicMixSnapshots(maxEntries = 2)
        snapshots.getOrLoad("a") { detail("a") }
        snapshots.getOrLoad("b") { detail("b") }
        snapshots.getOrLoad("c") { detail("c") }
        assertNull(snapshots.get("a"))
        assertEquals("b", snapshots.get("b")?.playlistId)
        assertEquals("c", snapshots.get("c")?.playlistId)
    }
}
