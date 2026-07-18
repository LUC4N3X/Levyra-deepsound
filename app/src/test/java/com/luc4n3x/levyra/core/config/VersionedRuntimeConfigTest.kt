package com.luc4n3x.levyra.core.config

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionedRuntimeConfigTest {
    @Test
    fun epochChangesOnlyWhenConfigurationChanges() {
        val config = VersionedRuntimeConfig("initial")

        assertEquals(0L, config.snapshot().epoch)
        assertEquals(0L, config.update("initial").epoch)
        assertEquals(1L, config.update("next").epoch)
        assertEquals(1L, config.update("next").epoch)
        assertEquals(2L, config.update("final").epoch)
    }

    @Test
    fun updatePublishesValueAndEpochAtomically() {
        val config = VersionedRuntimeConfig(listOf("apple-motion"))

        val updated = config.update(listOf("tidal-video-cover", "apple-motion"))
        val snapshot = config.snapshot()

        assertEquals(updated, snapshot)
        assertEquals(1L, snapshot.epoch)
        assertEquals(listOf("tidal-video-cover", "apple-motion"), snapshot.value)
    }
}
