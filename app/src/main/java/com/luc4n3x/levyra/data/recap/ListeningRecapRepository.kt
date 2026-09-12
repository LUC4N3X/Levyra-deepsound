package com.luc4n3x.levyra.data.recap

import com.luc4n3x.levyra.data.ListeningPulseStore
import com.luc4n3x.levyra.domain.recap.ListeningRecapEngine
import com.luc4n3x.levyra.domain.recap.ListeningRecapPeriod
import com.luc4n3x.levyra.domain.recap.ListeningRecapSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

class ListeningRecapRepository(
    private val pulseStore: ListeningPulseStore,
    private val zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() }
) {
    private val cache = ConcurrentHashMap<ListeningRecapPeriod, ListeningRecapSummary>()
    private val computeMutex = Mutex()

    suspend fun getRecap(
        period: ListeningRecapPeriod,
        force: Boolean = false
    ): ListeningRecapSummary = withContext(Dispatchers.IO) {
        if (!force) {
            val cached = cache[period]
            if (cached != null) return@withContext cached
        }

        computeMutex.withLock {
            if (!force) {
                val cached = cache[period]
                if (cached != null) return@withLock cached
            }

            val events = pulseStore.eventsWindow()
            val lifetime = if (period == ListeningRecapPeriod.AllTime) {
                pulseStore.lifetime()
            } else {
                null
            }

            val summary = withContext(Dispatchers.Default) {
                ListeningRecapEngine.build(
                    events = events,
                    period = period,
                    lifetime = lifetime,
                    nowMs = System.currentTimeMillis(),
                    zone = zoneIdProvider()
                )
            }

            cache[period] = summary
            summary
        }
    }

    fun peekCached(period: ListeningRecapPeriod): ListeningRecapSummary? = cache[period]

    fun invalidateCache() {
        cache.clear()
    }
}
