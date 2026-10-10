package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartOfflineTest {
    private val now = 1_800_000_000_000L
    private val enabled = LevyraSmartOfflineSettings(enabled = true)

    @Test
    fun `manual downloads are never selected for automatic eviction`() {
        val manual = stored("manual", DownloadOwnership.MANUAL, 90)
        val smart = stored("smart", DownloadOwnership.SMART_OFFLINE, 90)

        val plan = buildSmartOfflinePlan(
            ranked = listOf(ranked("new", 500, 90)),
            stored = listOf(manual, smart),
            storageLimitBytes = 90 * MIB,
            maxDownloads = 1
        )

        assertEquals(listOf("smart"), plan.evictions.map { it.trackId })
        assertEquals(listOf("new"), plan.downloads.map { it.candidate.track.id })
        assertFalse(manual in plan.evictions)
    }

    @Test
    fun `higher priority candidate replaces lowest smart download when full`() {
        val low = stored("low", DownloadOwnership.SMART_OFFLINE, 70)
        val plan = buildSmartOfflinePlan(
            ranked = listOf(ranked("new", 400, 70), ranked("low", 10, 70)),
            stored = listOf(low),
            storageLimitBytes = 70 * MIB,
            maxDownloads = 1
        )

        assertEquals("low", plan.evictions.single().trackId)
        assertEquals("new", plan.downloads.single().candidate.track.id)
        assertEquals(70 * MIB, plan.projectedBytes)
    }

    @Test
    fun `ranking is stable for identical input`() {
        val candidates = listOf(
            candidate("b", playCount = 3, lastPlayedAt = now - 1_000),
            candidate("a", playCount = 3, lastPlayedAt = now - 1_000)
        )

        val first = rankSmartOfflineCandidates(candidates, enabled, now)
        val second = rankSmartOfflineCandidates(candidates.reversed(), enabled, now)

        assertEquals(listOf("a", "b"), first.map { it.candidate.key })
        assertEquals(first, second)
    }

    @Test
    fun `favorites receive a strong priority bonus`() {
        val favorite = candidate("favorite", favorite = true)
        val ordinary = candidate("ordinary", playCount = 20)

        val ranked = rankSmartOfflineCandidates(listOf(ordinary, favorite), enabled, now)

        assertEquals("favorite", ranked.first().candidate.key)
        assertTrue(ranked.first().score > ranked.last().score)
    }

    @Test
    fun `storage projection never exceeds configured limit`() {
        val plan = buildSmartOfflinePlan(
            ranked = listOf(ranked("one", 300, 60), ranked("two", 200, 60)),
            stored = emptyList(),
            storageLimitBytes = 100 * MIB,
            maxDownloads = 4
        )

        assertEquals(listOf("one"), plan.downloads.map { it.candidate.track.id })
        assertTrue(plan.projectedBytes <= 100 * MIB)
    }

    @Test
    fun `wifi only and charging settings map to worker constraints`() {
        val constrained = smartOfflineConstraintSpec(
            enabled.copy(wifiOnly = true, chargingOnly = true)
        )
        val relaxed = smartOfflineConstraintSpec(
            enabled.copy(wifiOnly = false, chargingOnly = false)
        )

        assertTrue(constrained.requiresUnmeteredNetwork)
        assertTrue(constrained.requiresCharging)
        assertFalse(relaxed.requiresUnmeteredNetwork)
        assertFalse(relaxed.requiresCharging)
        assertTrue(constrained.requiresStorageNotLow)
        assertTrue(constrained.requiresBatteryNotLow)
    }

    @Test
    fun `existing track prevents duplicate download regardless of ownership`() {
        val existing = stored("same", DownloadOwnership.MANUAL, 40)
        val plan = buildSmartOfflinePlan(
            ranked = listOf(ranked("same", 500, 40)),
            stored = listOf(existing),
            storageLimitBytes = 100 * MIB,
            maxDownloads = 1
        )

        assertTrue(plan.downloads.isEmpty())
        assertTrue(plan.evictions.isEmpty())
    }

    @Test
    fun `active playback and active downloads are never selected for eviction`() {
        val playing = stored("playing", DownloadOwnership.SMART_OFFLINE, 70)
        val active = stored("active", DownloadOwnership.SMART_OFFLINE, 70)

        val plan = buildSmartOfflinePlan(
            ranked = listOf(ranked("new", 500, 70)),
            stored = listOf(playing, active),
            storageLimitBytes = 70 * MIB,
            protectedTrackIds = setOf("playing"),
            activeTrackKeys = setOf("active"),
            maxDownloads = 1
        )

        assertTrue(plan.evictions.isEmpty())
        assertTrue(plan.downloads.isEmpty())
    }

    @Test
    fun `cancelled and failed tasks may be retried but active manual task wins`() {
        assertTrue(canSmartOfflineEnqueue(DownloadOwnership.MANUAL, "CANCELLED", "work"))
        assertTrue(canSmartOfflineEnqueue(DownloadOwnership.MANUAL, "FAILED", "work"))
        assertFalse(canSmartOfflineEnqueue(DownloadOwnership.MANUAL, "RUNNING", "work"))
        assertTrue(canSmartOfflineEnqueue(DownloadOwnership.SMART_OFFLINE, "RUNNING", "work"))
    }

    @Test
    fun `empty history performs no destructive planning`() {
        val smart = stored("kept", DownloadOwnership.SMART_OFFLINE, 200)

        val plan = buildSmartOfflinePlan(emptyList(), listOf(smart), storageLimitBytes = 100 * MIB)

        assertTrue(plan.downloads.isEmpty())
        assertTrue(plan.evictions.isEmpty())
        assertEquals(200 * MIB, plan.projectedBytes)
    }

    @Test
    fun `disabled smart offline and active manual downloads prevent a run`() {
        assertFalse(shouldRunSmartOffline(enabled.copy(enabled = false), 4, false))
        assertFalse(shouldRunSmartOffline(enabled, 0, false))
        assertFalse(shouldRunSmartOffline(enabled, 4, true))
        assertTrue(shouldRunSmartOffline(enabled, 4, false))
    }

    @Test
    fun `unknown persisted ownership is protected as manual`() {
        assertEquals(DownloadOwnership.MANUAL, DownloadOwnership.fromStorage(""))
        assertEquals(DownloadOwnership.MANUAL, DownloadOwnership.fromStorage("legacy"))
    }

    private fun ranked(id: String, score: Int, size: Long): RankedSmartOfflineCandidate =
        RankedSmartOfflineCandidate(candidate(id, estimatedSizeBytes = size * MIB), score)

    private fun stored(id: String, ownership: DownloadOwnership, size: Long): SmartOfflineStoredItem =
        SmartOfflineStoredItem(
            id = id.hashCode().toLong(),
            trackKey = id,
            trackId = id,
            ownership = ownership,
            sizeBytes = size * MIB
        )

    private fun candidate(
        id: String,
        playCount: Int = 0,
        lastPlayedAt: Long = 0,
        favorite: Boolean = false,
        estimatedSizeBytes: Long = 10
    ): SmartOfflineCandidate = SmartOfflineCandidate(
        key = id,
        track = track(id),
        playCount = playCount,
        recentPlayCount = 0,
        firstPlayedAt = 0,
        lastPlayedAt = lastPlayedAt,
        favorite = favorite,
        playlistCount = 0,
        recentlyAddedAt = 0,
        estimatedSizeBytes = estimatedSizeBytes
    )

    private fun track(id: String): Track = Track(
        id = id,
        title = id,
        artist = "Artist",
        album = "Album",
        durationMs = 180_000,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0
    )

    private companion object {
        const val MIB = 1024L * 1024L
    }
}
