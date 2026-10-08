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
        synchronized(lock) {
            if (pending != value) return
            write(value)
            pending = null
        }
    }

    fun flush() {
        synchronized(lock) {
            pending?.let(write)
            pending = null
        }
    }
}
