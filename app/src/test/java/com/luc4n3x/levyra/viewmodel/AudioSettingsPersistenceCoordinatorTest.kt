package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.LevyraAudioSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioSettingsPersistenceCoordinatorTest {
    @Test
    fun staleWriteCannotReplaceLatestSavedPresets() {
        val writes = mutableListOf<LevyraAudioSettings>()
        val coordinator = AudioSettingsPersistenceCoordinator(writes::add)
        val old = LevyraAudioSettings()
        val latest = old.copy(customPresets = (1..12).map { index ->
            com.luc4n3x.levyra.domain.LevyraAudioPreset(
                id = "custom_$index", fallbackLabel = "Headphones $index",
                levels = List(10) { 0 }, bassBoost = 0, virtualizer = 0
            )
        })
        coordinator.schedule(old)
        coordinator.schedule(latest)
        coordinator.persist(latest)
        coordinator.persist(old)
        coordinator.flush()

        assertEquals(listOf(latest), writes)
    }

    @Test
    fun cleanupFlushesLatestPendingAudioSettings() {
        val writes = mutableListOf<LevyraAudioSettings>()
        val coordinator = AudioSettingsPersistenceCoordinator(writes::add)
        val first = LevyraAudioSettings(preampDb = -2f)
        val latest = LevyraAudioSettings(preampDb = 1f)

        coordinator.schedule(first)
        coordinator.schedule(latest)
        coordinator.flush()
        coordinator.flush()

        assertEquals(listOf(latest), writes)
    }
}
