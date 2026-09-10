package com.luc4n3x.levyra.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

internal class LevyraPreferencesStore(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope
) {
    private class PendingWrite(val block: (MutablePreferences) -> Unit) {
        val completion = CompletableDeferred<Unit>()
    }

    private class DerivedValues(val source: Preferences) {
        val values = ConcurrentHashMap<String, Any>()
    }

    private val lock = Any()
    private val initialLoad = CountDownLatch(1)
    private val state = MutableStateFlow<Preferences?>(null)
    private val writesBeforeLoad = ArrayList<(MutablePreferences) -> Unit>()
    private val pendingWrites = Channel<PendingWrite>(Channel.UNLIMITED)

    @Volatile
    private var derivedValues = DerivedValues(emptyPreferences())

    val preferences: Flow<Preferences> = state.filterNotNull()

    init {
        scope.launch {
            publishInitial(readPersisted())
            for (write in pendingWrites) persist(write)
        }
    }

    fun current(): Preferences {
        state.value?.let { return it }
        initialLoad.await()
        return checkNotNull(state.value)
    }

    suspend fun awaitCurrent(): Preferences = preferences.first()

    fun <T : Any> derived(key: String, compute: (Preferences) -> T): T {
        val source = current()
        val cache = derivedValues.takeIf { it.source === source }
            ?: DerivedValues(source).also { derivedValues = it }
        @Suppress("UNCHECKED_CAST")
        return cache.values.getOrPut(key) { compute(source) } as T
    }

    fun edit(block: (MutablePreferences) -> Unit): Deferred<Unit> {
        val write = PendingWrite(block)
        synchronized(lock) {
            val loaded = state.value
            if (loaded == null) {
                writesBeforeLoad += block
            } else {
                state.value = loaded.edited(listOf(block))
            }
            pendingWrites.trySend(write)
        }
        return write.completion
    }

    suspend fun commit(block: (MutablePreferences) -> Unit) {
        edit(block).await()
    }

    private suspend fun readPersisted(): Preferences = try {
        dataStore.data.first()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Timber.w(error, "DataStore read failed")
        emptyPreferences()
    }

    private fun publishInitial(persisted: Preferences) {
        synchronized(lock) {
            state.value = persisted.edited(writesBeforeLoad)
            writesBeforeLoad.clear()
        }
        initialLoad.countDown()
    }

    private suspend fun persist(write: PendingWrite) {
        try {
            dataStore.edit(write.block)
            write.completion.complete(Unit)
        } catch (error: CancellationException) {
            write.completion.cancel(error)
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "DataStore write failed")
            write.completion.completeExceptionally(error)
        }
    }

    private fun Preferences.edited(blocks: List<(MutablePreferences) -> Unit>): Preferences {
        if (blocks.isEmpty()) return this
        return toMutablePreferences().apply { blocks.forEach { block -> block(this) } }.toPreferences()
    }
}
