package com.luc4n3x.levyra.data.recap

import com.luc4n3x.levyra.domain.LifetimeListening
import com.luc4n3x.levyra.domain.ListenEvent
import com.luc4n3x.levyra.domain.ListenIdentity
import com.luc4n3x.levyra.domain.recap.ListeningRecapEngine
import com.luc4n3x.levyra.domain.recap.ListeningRecapPeriod
import com.luc4n3x.levyra.domain.recap.ListeningRecapSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

interface ListeningPulseDataSource {
    suspend fun eventsWindow(days: Int = 365): List<ListenEvent>
    suspend fun lifetime(): LifetimeListening
    suspend fun firstPlayedByKey(trackKeys: List<String>): Map<String, Long> = emptyMap()
}

class ListeningRecapRepository(
    private val pulseStore: ListeningPulseDataSource,
    private val zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() }
) {
    private data class CachedRecap(
        val date: LocalDate,
        val zoneId: ZoneId,
        val summary: ListeningRecapSummary
    )

    private val cache = ConcurrentHashMap<ListeningRecapPeriod, CachedRecap>()
    private val computeMutex = Mutex()
    private val generation = AtomicLong(0L)

    private var cachedEventsSnapshot: List<ListenEvent>? = null
    private var snapshotGeneration: Long = -1L
    private var snapshotDate: LocalDate? = null
    private var snapshotZone: ZoneId? = null

    suspend fun getRecap(
        period: ListeningRecapPeriod,
        force: Boolean = false
    ): ListeningRecapSummary = withContext(Dispatchers.IO) {
        val currentGen = generation.get()
        val zone = zoneIdProvider()
        val nowMs = System.currentTimeMillis()
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()

        if (!force) {
            val cached = cache[period]
            if (cached != null && cached.date == today && cached.zoneId == zone) return@withContext cached.summary
        }

        computeMutex.withLock {
            val genAtLock = generation.get()
            if (!force && genAtLock == currentGen) {
                val cached = cache[period]
                if (cached != null && cached.date == today && cached.zoneId == zone) return@withLock cached.summary
            }

            val events = if (cachedEventsSnapshot != null && snapshotGeneration == genAtLock && snapshotDate == today && snapshotZone == zone) {
                cachedEventsSnapshot!!
            } else {
                val loaded = pulseStore.eventsWindow()
                cachedEventsSnapshot = loaded
                snapshotGeneration = genAtLock
                snapshotDate = today
                snapshotZone = zone
                loaded
            }

            val lifetime = if (period == ListeningRecapPeriod.AllTime) {
                pulseStore.lifetime()
            } else {
                null
            }

            val firstPlayedMap = if (period == ListeningRecapPeriod.Days7 || period == ListeningRecapPeriod.Days30) {
                val trackKeys = events.map { ListenIdentity.trackKey(it) }.distinct()
                pulseStore.firstPlayedByKey(trackKeys)
            } else {
                emptyMap()
            }

            val summary = withContext(Dispatchers.Default) {
                ListeningRecapEngine.build(
                    events = events,
                    period = period,
                    lifetime = lifetime,
                    firstPlayedMap = firstPlayedMap,
                    nowMs = nowMs,
                    zone = zone
                )
            }

            if (generation.get() == genAtLock) {
                cache[period] = CachedRecap(today, zone, summary)
            }
            summary
        }
    }

    fun peekCached(period: ListeningRecapPeriod): ListeningRecapSummary? {
        val zone = zoneIdProvider()
        val today = LocalDate.now(zone)
        val entry = cache[period] ?: return null
        return if (entry.date == today && entry.zoneId == zone) entry.summary else null
    }

    fun invalidateCache() {
        generation.incrementAndGet()
        cache.clear()
        cachedEventsSnapshot = null
        snapshotGeneration = -1L
        snapshotDate = null
        snapshotZone = null
    }
}
