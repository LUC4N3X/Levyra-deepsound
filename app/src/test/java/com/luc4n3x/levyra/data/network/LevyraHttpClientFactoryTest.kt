package com.luc4n3x.levyra.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraHttpClientFactoryTest {
    @Test
    fun downloadClientBoundsPerHostPressureAndStallTime() {
        val client = LevyraHttpClientFactory.download()

        assertEquals(32, client.dispatcher.maxRequests)
        assertEquals(8, client.dispatcher.maxRequestsPerHost)
        assertEquals(6_000, client.connectTimeoutMillis)
        assertEquals(15_000, client.readTimeoutMillis)
        assertTrue(client.retryOnConnectionFailure)
    }

    @Test
    fun downloadClientKeepsLongHealthyTransfersUnboundedByTotalCallTime() {
        val client = LevyraHttpClientFactory.download()

        assertEquals(0, client.callTimeoutMillis)
    }
}
