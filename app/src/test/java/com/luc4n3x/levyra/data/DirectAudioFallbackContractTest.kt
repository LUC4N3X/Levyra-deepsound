package com.luc4n3x.levyra.data

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectAudioFallbackContractTest {
    @Test
    fun directAudioValidatesRankedCandidatesBeforeAcceptingOne() {
        val resolver = readSource("data/PlaybackResolver.kt")
        val function = resolver.indexOf("private suspend fun resolveWithInnerTubeOnce")
        val candidates = resolver.indexOf("val audioCandidates = buildList", function)
        val loop = resolver.indexOf("for ((format, _, label) in audioCandidates)", candidates)
        val strictProbe = resolver.indexOf("!verifyDirectAudioUrlFast(", loop)
        val identity = resolver.indexOf("identity = clientIdentity", strictProbe)
        val strictValidation = resolver.indexOf("trustAttestedGoogleVideo = false", identity)
        val assignment = resolver.indexOf("bestAudioUrl = url", loop)

        assertTrue(function >= 0)
        assertTrue(candidates > function)
        assertTrue(loop > candidates)
        assertTrue(strictProbe > loop)
        assertTrue(identity > strictProbe)
        assertTrue(strictValidation > identity)
        assertTrue(assignment > strictValidation)
    }

    @Test
    fun strictCandidateProbeIsLimitedToNormalAudioFallback() {
        val resolver = readSource("data/PlaybackResolver.kt")
        val function = resolver.indexOf("private suspend fun resolveWithInnerTubeOnce")
        val candidates = resolver.indexOf("val audioCandidates = buildList", function)
        val loop = resolver.indexOf("for ((format, _, label) in audioCandidates)", candidates)
        val assignment = resolver.indexOf("bestAudioUrl = url", loop)
        val candidateProbe = resolver.substring(loop, assignment)

        assertTrue(candidateProbe.contains("if (!isVideoMode && !preferMp4Audio &&"))
        assertTrue(candidateProbe.contains("identity = clientIdentity"))
        assertTrue(candidateProbe.contains("trustAttestedGoogleVideo = false"))
        assertTrue(resolver.contains("trustAttestedGoogleVideo: Boolean = true"))
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
