package com.luc4n3x.levyra.data

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePlaybackConcurrencyContractTest {
    @Test
    fun decoderCallerProvenanceIsResetAtResolutionSecurityBoundaries() {
        val decoder = readSource("data/YoutubeLocalDecoder.kt")
        val security = readSource("data/YoutubePlaybackSecurity.kt")

        assertTrue(decoder.contains("internal fun clearCallerProvenance()"))

        val cachedSession = security.substring(
            security.indexOf("fun cachedSession()"),
            security.indexOf("private fun readCachedSession()")
        )
        assertTrue(cachedSession.contains("YoutubeLocalDecoder.clearCallerProvenance()"))

        val poTokens = security.substring(
            security.indexOf("suspend fun poTokensForPlayback("),
            security.indexOf("suspend fun rotateIfNeeded(")
        )
        assertTrue(poTokens.contains("finally"))
        assertTrue(poTokens.contains("YoutubeLocalDecoder.clearCallerProvenance()"))
    }

    @Test
    fun securityFailureAttemptsAreGenerationFencedInsideRotationMutex() {
        val security = readSource("data/YoutubePlaybackSecurity.kt")
        val rotate = security.substring(
            security.indexOf("suspend fun rotateIfNeeded("),
            security.indexOf("fun resetFailureState()")
        )
        val mutex = rotate.indexOf("sessionMutex.withLock")
        val generationCheck = rotate.indexOf("session.generation != expectedGeneration")
        val attempt = rotate.indexOf("recordFailureAttempt(session.generation)")
        val stateFence = rotate.indexOf("latestGeneration != session.generation")

        assertTrue(mutex >= 0)
        assertTrue(generationCheck > mutex)
        assertTrue(attempt > generationCheck)
        assertTrue(stateFence > attempt)
        assertTrue(security.contains("private val failureGeneration = AtomicLong(-1L)"))
        assertTrue(security.contains("resetFailureStateForGeneration(generation)"))
    }

    @Test
    fun rejectedAnalyzerIdentityTrackingRemainsBounded() {
        val decoder = readSource("data/YoutubeLocalDecoder.kt")
        val reject = decoder.substring(
            decoder.indexOf("suspend fun rejectAnalyzedConfig("),
            decoder.indexOf("private suspend fun resolvePlayer(")
        )

        assertTrue(reject.contains("rejectedAnalyzerIdentities.size >= MAX_TRACKED_CONFIG_IDENTITIES"))
        assertTrue(reject.contains("rejectedAnalyzerIdentities.clear()"))
    }

    private fun readSource(relativePath: String): String =
        Files.readString(sourceFile(relativePath)).replace("\r\n", "\n")

    private fun sourceFile(relativePath: String): Path {
        return sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
            Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
        ).firstOrNull(Files::exists) ?: error("Source file not found: $relativePath")
    }
}
