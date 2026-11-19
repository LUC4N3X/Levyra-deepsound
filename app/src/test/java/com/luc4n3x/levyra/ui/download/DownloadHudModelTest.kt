package com.luc4n3x.levyra.ui.download

import com.luc4n3x.levyra.domain.OfflineDownloadTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadHudModelTest {
    private fun task(key: String, state: String, progress: Int = 0) =
        OfflineDownloadTask(taskKey = key, trackId = key, title = key, artist = "", state = state, progress = progress, error = "")

    @Test
    fun countsCompletedTracksAcrossTheSession() {
        val first = activeHudTasks(listOf(task("a", "RUNNING", 50), task("b", "QUEUED"), task("c", "QUEUED")))
        val session = extendDownloadSession(emptyMap(), first)
        val later = activeHudTasks(listOf(task("c", "RUNNING", 40), task("d", "FAILED")))
        val snapshot = downloadHudSnapshot(later, extendDownloadSession(session, later), setOf("a", "b"))!!

        assertEquals("c", snapshot.current.taskKey)
        assertEquals(40, snapshot.currentProgress)
        assertEquals(2, snapshot.completed)
        assertEquals(3, snapshot.total)
        assertEquals(80, snapshot.overallProgress)
        assertEquals(0, downloadHudSnapshot(later, extendDownloadSession(session, later), emptySet())!!.completed)
        assertEquals(emptyList<OfflineDownloadTask>(), snapshot.queued)
    }

    @Test
    fun picksTheRunningTaskAndListsTheRestAsQueued() {
        val active = activeHudTasks(listOf(task("q", "QUEUED"), task("r", "RUNNING", 10), task("x", "PAUSED")))
        val snapshot = downloadHudSnapshot(active, emptyMap(), emptySet())!!

        assertEquals("r", snapshot.current.taskKey)
        assertEquals(listOf("q"), snapshot.queued.map { it.taskKey })
        assertEquals(0, snapshot.completed)
        assertEquals(2, snapshot.total)
    }

    @Test
    fun emptyQueueEndsTheSession() {
        assertNull(downloadHudSnapshot(emptyList(), mapOf("a" to "a"), emptySet()))
        assertEquals(emptyMap<String, String>(), extendDownloadSession(mapOf("a" to "a"), emptyList()))
    }
}
