package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.LevyraAudioSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioSettingsPersistenceCoordinatorTest {
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
