package com.luc4n3x.levyra.data.hqaudio

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal interface AudioCredentialsCache {
    fun read(): String?
    fun write(document: String)
}

internal class AudioCredentialsStore(
    private val fetch: suspend () -> String,
    private val cache: AudioCredentialsCache? = null,
    private val clock: () -> Long = System::currentTimeMillis
) : AudioCredentialsSource {
    private val loadMutex = Mutex()
    private val refreshMutex = Mutex()
    @Volatile private var snapshot = AudioCredentials.embedded()
    private var loaded = false
    private var lastRefresh: Long? = null
    private var lastAuthRefresh: Long? = null
    private var refreshInterval = REFRESH_INTERVAL_MS

    override suspend fun current(): AudioCredentials = withContext(Dispatchers.IO) {
        loadMutex.withLock { load(); snapshot }
    }

    suspend fun refresh(): AudioCredentials = refresh(null)

    override suspend fun refreshAfterRejection(rejected: AudioCredentials): AudioCredentials = refresh(rejected)

    private suspend fun refresh(rejected: AudioCredentials?): AudioCredentials = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            current()
            val now = clock()
            if (!mayRefresh(rejected, now)) return@withLock snapshot
            val previousRefresh = lastRefresh
            val previousAuthRefresh = lastAuthRefresh
            val previousInterval = refreshInterval
            lastRefresh = now
            refreshInterval = AUTH_REFRESH_INTERVAL_MS
            if (rejected != null) lastAuthRefresh = now
            try {
                fetchAndPublish()
            } catch (cancelled: CancellationException) {
                lastRefresh = previousRefresh
                lastAuthRefresh = previousAuthRefresh
                refreshInterval = previousInterval
                throw cancelled
            } catch (_: Exception) { }
            snapshot
        }
    }

    private fun mayRefresh(rejected: AudioCredentials?, now: Long): Boolean {
        if (rejected != null && rejected !== snapshot) return false
        val last = if (rejected == null) lastRefresh else lastAuthRefresh
        val interval = if (rejected == null) refreshInterval else AUTH_REFRESH_INTERVAL_MS
        return last == null || now < last || now - last >= interval
    }

    private suspend fun fetchAndPublish() {
        val document = fetch()
        val next = AudioCredentials.parse(document) ?: return
        if (next.revision < snapshot.revision) return
        refreshInterval = REFRESH_INTERVAL_MS
        if (next.revision == snapshot.revision) return
        persist(document)
        snapshot = next
    }

    private fun persist(document: String) {
        try { cache?.write(document) } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) { }
    }

    private fun load() {
        if (loaded) return
        loaded = true
        val cached = try { cache?.read()?.let(AudioCredentials::parse) } catch (cancelled: CancellationException) {
            loaded = false
            throw cancelled
        } catch (_: Exception) { null }
        if (cached != null && cached.revision > snapshot.revision) snapshot = cached
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 60 * 60 * 1000L
        private const val AUTH_REFRESH_INTERVAL_MS = 60 * 1000L
    }
}
