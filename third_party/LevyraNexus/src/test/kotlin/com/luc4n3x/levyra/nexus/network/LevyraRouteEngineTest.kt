package com.luc4n3x.levyra.nexus.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraRouteEngineTest {
    @Test
    fun fasterReliableRouteMovesFirst() {
        var now = 1_000L
        val engine = LevyraRouteEngine { now }
        val slow = LevyraRoute("slow", "example.com", LevyraTransport.OKHTTP, LevyraAddressFamily.IPV4, 0)
        val fast = LevyraRoute("fast", "example.com", LevyraTransport.OKHTTP, LevyraAddressFamily.IPV6, 1)

        repeat(4) { engine.recordSuccess(slow, 800L) }
        repeat(4) { engine.recordSuccess(fast, 120L) }

        assertEquals(fast, engine.order(listOf(slow, fast)).first())
    }

    @Test
    fun rateLimitedRouteIsBlockedWithoutAffectingOthers() {
        var now = 10_000L
        val engine = LevyraRouteEngine { now }
        val blocked = LevyraRoute("blocked", "example.com")
        val healthy = LevyraRoute("healthy", "example.com", priority = 1)

        engine.recordFailure(blocked, LevyraRouteFailure.RATE_LIMIT)

        assertEquals(listOf(healthy), engine.order(listOf(blocked, healthy)))
        now += 11L * 60L * 1_000L
        assertTrue(blocked in engine.order(listOf(blocked, healthy)))
    }

    @Test
    fun recoveredDenialDoesNotGloballyBlockRoute() {
        val engine = LevyraRouteEngine { 5_000L }
        val denied = LevyraRoute("denied", "example.com")
        val fallback = LevyraRoute("fallback", "example.com", priority = 1)

        repeat(5) {
            engine.recordFailure(denied, LevyraRouteFailure.ACCESS_DENIED, recoveredByFallback = true)
            engine.recordSuccess(fallback, 100L)
        }

        assertTrue(denied in engine.order(listOf(denied, fallback)))
        assertEquals(fallback, engine.order(listOf(denied, fallback)).first())
    }

    @Test
    fun staleSuccessDoesNotEraseActiveRateLimitBlock() {
        val engine = LevyraRouteEngine { 10_000L }
        val route = LevyraRoute("route", "example.com")
        engine.recordFailure(route, LevyraRouteFailure.RATE_LIMIT)
        engine.recordSuccess(route, 100L)
        assertTrue(engine.order(listOf(route)).isEmpty())
    }

    @Test
    fun networkResetClearsTemporaryBlocks() {
        val engine = LevyraRouteEngine { 1_000L }
        val route = LevyraRoute("route", "example.com")
        engine.recordFailure(route, LevyraRouteFailure.RATE_LIMIT)
        assertTrue(engine.order(listOf(route)).isEmpty())
        engine.resetVolatileState()
        assertEquals(listOf(route), engine.order(listOf(route)))
    }
}
