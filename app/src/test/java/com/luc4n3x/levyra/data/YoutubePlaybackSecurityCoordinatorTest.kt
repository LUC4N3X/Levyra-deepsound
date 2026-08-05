package com.luc4n3x.levyra.data

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePlaybackSecurityCoordinatorTest {

    @Test
    fun poTokenWaitBudgetBoundsTheWholeOperation() {
        val error = runCatching {
            runBlocking {
                withPoTokenWaitBudget(25L) {
                    delay(250L)
                    "unreachable"
                }
            }
        }.exceptionOrNull()

        assertTrue(error is YoutubePoTokenRuntimeUnavailableException)
    }

    @Test
    fun outerTimeoutIsNotConvertedIntoALocalRuntimeFailure() {
        val error = runCatching {
            runBlocking {
                withTimeout(25L) {
                    withPoTokenWaitBudget(1_000L) {
                        delay(250L)
                        "unreachable"
                    }
                }
            }
        }.exceptionOrNull()

        assertTrue(error is TimeoutCancellationException)
        assertFalse(error is YoutubePoTokenRuntimeUnavailableException)
    }

    @Test
    fun successfulWorkReturnsThroughTheBudgetWrapper() = runBlocking {
        assertEquals("ready", withPoTokenWaitBudget(500L) { "ready" })
    }

    @Test
    fun invalidationRetiresOnlyOlderSessions() {
        assertTrue(PoTokenInvalidationPolicy.shouldRetire(sessionVersion = 4L, invalidatedVersion = 5L))
        assertFalse(PoTokenInvalidationPolicy.shouldRetire(sessionVersion = 5L, invalidatedVersion = 5L))
        assertFalse(PoTokenInvalidationPolicy.shouldRetire(sessionVersion = 6L, invalidatedVersion = 5L))
    }

    @Test
    fun replacementIsJoinedWhenTheActiveSessionCannotServe() {
        assertTrue(
            PoTokenReplacementPolicy.shouldJoin(
                hasReplacement = true,
                replacementMatches = true,
                replacementUsable = true,
                hasActive = true,
                activeDiscarded = true,
                activeMatches = true,
                activeUsable = true
            )
        )
        assertTrue(
            PoTokenReplacementPolicy.shouldJoin(
                hasReplacement = true,
                replacementMatches = true,
                replacementUsable = true,
                hasActive = true,
                activeDiscarded = false,
                activeMatches = false,
                activeUsable = true
            )
        )
        assertTrue(
            PoTokenReplacementPolicy.shouldJoin(
                hasReplacement = true,
                replacementMatches = true,
                replacementUsable = true,
                hasActive = true,
                activeDiscarded = false,
                activeMatches = true,
                activeUsable = false
            )
        )
    }

    @Test
    fun healthyActiveSessionWinsOverAReplacementBuild() {
        assertFalse(
            PoTokenReplacementPolicy.shouldJoin(
                hasReplacement = true,
                replacementMatches = true,
                replacementUsable = true,
                hasActive = true,
                activeDiscarded = false,
                activeMatches = true,
                activeUsable = true
            )
        )
    }

    @Test
    fun unusableOrMismatchedReplacementIsNeverJoined() {
        assertFalse(
            PoTokenReplacementPolicy.shouldJoin(
                hasReplacement = true,
                replacementMatches = false,
                replacementUsable = true,
                hasActive = false,
                activeDiscarded = false,
                activeMatches = false,
                activeUsable = false
            )
        )
        assertFalse(
            PoTokenReplacementPolicy.shouldJoin(
                hasReplacement = true,
                replacementMatches = true,
                replacementUsable = false,
                hasActive = false,
                activeDiscarded = false,
                activeMatches = false,
                activeUsable = false
            )
        )
    }

    @Test
    fun ownedCancellationArmsBackoffButScopeShutdownDoesNot() {
        assertTrue(
            PoTokenBuildFailurePolicy.shouldArmBackoff(
                cancellation = true,
                ownerActive = true
            )
        )
        assertFalse(
            PoTokenBuildFailurePolicy.shouldArmBackoff(
                cancellation = true,
                ownerActive = false
            )
        )
        assertTrue(
            PoTokenBuildFailurePolicy.shouldArmBackoff(
                cancellation = false,
                ownerActive = false
            )
        )
    }
}
