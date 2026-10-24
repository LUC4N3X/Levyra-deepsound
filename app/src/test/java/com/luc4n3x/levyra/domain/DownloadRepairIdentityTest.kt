package com.luc4n3x.levyra.domain

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DownloadRepairIdentityTest {
    @Test fun durablePayloadRetainsRepairIntentWithoutWorkManagerMetadata() {
        val payload = withDownloadRepairIdentity("{\"id\":\"song\",\"title\":\"Music\"}", 42L)
        assertEquals(42L, downloadRepairIdentity(payload))
        assertEquals("song", JSONObject(payload).getString("id"))
        assertEquals("Music", JSONObject(payload).getString("title"))
    }

    @Test fun ordinaryAndMalformedPayloadsDoNotBecomeRepairs() {
        assertEquals(0L, downloadRepairIdentity("{}"))
        assertEquals(0L, downloadRepairIdentity("invalid"))
        assertEquals("{}", withDownloadRepairIdentity("{}", 0L))
    }

    @Test fun separateSavedCopiesReceiveIndependentTasks() {
        assertNotEquals(downloadRepairTaskKey(1L), downloadRepairTaskKey(2L))
        assertEquals(downloadRepairTaskKey(1L), downloadRepairTaskKey(1L))
    }
}
