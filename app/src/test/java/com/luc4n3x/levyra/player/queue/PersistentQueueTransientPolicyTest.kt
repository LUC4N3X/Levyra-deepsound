package com.luc4n3x.levyra.player.queue

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentQueueTransientPolicyTest {
    @Test
    fun normalQueueMutationsRemainDurable() {
        assertTrue(queuePersistenceAllowed(transientPlaybackActive = false))
    }

    @Test
    fun samplesTransientMutationsCanNeverBePersisted() {
        assertFalse(queuePersistenceAllowed(transientPlaybackActive = true))
    }
}
