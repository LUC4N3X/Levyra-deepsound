package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.LevyraAudioSettings

internal class AudioSettingsPersistenceCoordinator(
    private val write: (LevyraAudioSettings) -> Unit
) {
    private val lock = Any()
    private var pending: LevyraAudioSettings? = null

    fun schedule(value: LevyraAudioSettings) {
        synchronized(lock) { pending = value }
    }

    fun persist(value: LevyraAudioSettings) {
        write(value)
        synchronized(lock) {
            if (pending == value) pending = null
        }
    }

    fun flush() {
        val value = synchronized(lock) {
            pending.also { pending = null }
        }
        value?.let(write)
    }
}
