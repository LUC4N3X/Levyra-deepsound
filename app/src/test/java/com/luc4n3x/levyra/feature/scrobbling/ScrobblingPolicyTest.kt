package com.luc4n3x.levyra.feature.scrobbling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScrobblingPolicyTest {
    @Test fun `short and unknown tracks do not get a scrobble threshold`() {
        assertNull(scrobbleThresholdMs(0))
        assertNull(scrobbleThresholdMs(29_999))
    }
    @Test fun `threshold is half duration capped at four minutes`() {
        assertEquals(30_000L, scrobbleThresholdMs(60_000))
        assertEquals(240_000L, scrobbleThresholdMs(900_000))
    }
}
