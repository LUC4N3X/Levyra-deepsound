package com.luc4n3x.levyra.data

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectAudioFallbackContractTest {
    @Test
    fun directAudioValidatesRankedCandidatesBeforeAcceptingOne() {
        val resolver = Files.readString(sourceFile("data/PlaybackResolver.kt"))
        val function = resolver.indexOf("private suspend fun resolveWithInnerTubeOnce")
        val candidates = resolver.indexOf("val audioCandidates = buildList", function)
        val loop = resolver.indexOf("for ((format, _, label) in audioCandidates)", candidates)
        val strictProbe = resolver.indexOf(
            "!verifyDirectAudioUrlFast(url, trustAttestedGoogleVideo = false)",
            loop
        )
        val assignment = resolver.indexOf("bestAudioUrl = url", loop)

        assertTrue(function >= 0)
        assertTrue(candidates > function)
        assertTrue(loop > candidates)
        assertTrue(strictProbe > loop)
        assertTrue(assignment > strictProbe)
    }

    @Test
    fun strictCandidateProbeIsLimitedToNormalAudioFallback() {
        val resolver = Files.readString(sourceFile("data/PlaybackResolver.kt"))

        assertTrue(
            resolver.contains(
                "if (!isVideoMode && !preferMp4Audio &&\n" +
                    "                    !verifyDirectAudioUrlFast(url, trustAttestedGoogleVideo = false)"
            )
        )
        assertTrue(resolver.contains("trustAttestedGoogleVideo: Boolean = true"))
    }

    private fun sourceFile(relativePath: String): Path {
        return sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
            Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
        ).firstOrNull(Files::exists) ?: error("Source file not found: $relativePath")
    }
}
