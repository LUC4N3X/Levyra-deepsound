package com.luc4n3x.levyra.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
    private val optimisticWrites = ArrayList<PendingWrite>()
    private val pendingWrites = Channel<PendingWrite>(Channel.UNLIMITED)

    private var persistedState: Preferences? = null

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
            optimisticWrites += write
            state.value?.let { loaded ->
                state.value = loaded.edited(listOf(block))
            }
            pendingWrites.trySend(write)
        }
        return write.completion
    }

    suspend fun commit(block: (MutablePreferences) -> Unit) {
        edit(block).await()
    }

    private suspend fun readPersisted(): Preferences {
        repeat(3) { attempt ->
            try {
                return dataStore.data.first()
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                Timber.w(error, "DataStore read failed (attempt %d)", attempt + 1)
                if (attempt < 2) delay(75L * (attempt + 1))
            } catch (error: Throwable) {
                Timber.w(error, "DataStore read failed")
                return emptyPreferences()
            }
        }
        return emptyPreferences()
    }

    private fun publishInitial(persisted: Preferences) {
        synchronized(lock) {
            persistedState = persisted
            state.value = persisted.edited(optimisticWrites.map { it.block })
        }
        initialLoad.countDown()
    }

    private suspend fun persist(write: PendingWrite) {
        try {
            val persisted = dataStore.edit(write.block)
            synchronized(lock) {
                persistedState = persisted
                optimisticWrites.remove(write)
                state.value = persisted.edited(optimisticWrites.map { it.block })
            }
            write.completion.complete(Unit)
        } catch (error: CancellationException) {
            write.completion.cancel(error)
            throw error
        } catch (error: Throwable) {
            synchronized(lock) {
                optimisticWrites.remove(write)
                persistedState?.let { persisted ->
                    state.value = persisted.edited(optimisticWrites.map { it.block })
                }
            }
            Timber.w(error, "DataStore write failed")
            write.completion.completeExceptionally(error)
        }
    }

    private fun Preferences.edited(blocks: List<(MutablePreferences) -> Unit>): Preferences {
        if (blocks.isEmpty()) return this
        return toMutablePreferences().apply { blocks.forEach { block -> block(this) } }.toPreferences()
    }
}
